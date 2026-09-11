package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.dto.QuoteQueryDTO;
import com.freight.entity.FreightQuote;
import com.freight.entity.QuoteUploadLog;
import com.freight.entity.FreightVesselSchedule;
import com.freight.mapper.FreightQuoteMapper;
import com.freight.mapper.QuoteUploadLogMapper;
import com.freight.service.QuoteService;
import com.freight.service.VesselScheduleService;
import com.freight.util.QuoteExcelParser;
import com.freight.util.VesselScheduleExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final FreightQuoteMapper quoteMapper;
    private final QuoteUploadLogMapper uploadLogMapper;
    private final VesselScheduleService vesselScheduleService;

    @Override
    @Transactional
    public QuoteUploadLog uploadAndParse(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        LocalDate[] dates = QuoteExcelParser.parseDateFromFileName(fileName != null ? fileName : "");
        LocalDate validFrom = dates[0];
        LocalDate validTo = dates[1];

        // 解析Excel
        List<FreightQuote> parsed;
        try {
            parsed = QuoteExcelParser.parse(file.getInputStream(), fileName, validFrom, validTo);
        } catch (Exception e) {
            log.error("Excel解析失败", e);
            throw new BusinessException("Excel解析失败: " + e.getMessage());
        }

        if (parsed.isEmpty()) {
            throw new BusinessException("未解析到任何报价数据，请检查文件格式");
        }

        // 只匹配相同有效期，避免历史报价影响本次导入
        List<FreightQuote> existing = quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>()
                .eq(FreightQuote::getDeleted, 0)
                .eq(FreightQuote::getValidFrom, validFrom)
                .eq(FreightQuote::getValidTo, validTo)
        );

        // 建立唯一key → 已有记录的 Map
        // key = sourceSheet + destination + volumeRange + via
        Map<String, FreightQuote> existingMap = existing.stream()
            .collect(Collectors.toMap(
                QuoteServiceImpl::buildKey,
                q -> q,
                (a, b) -> a  // 有重复取第一个
            ));

        // 同一批次的重复行只处理一次，避免合并单元格辅助行重复更新同一记录
        Map<String, FreightQuote> parsedMap = new LinkedHashMap<>();
        for (FreightQuote q : parsed) {
            parsedMap.putIfAbsent(buildKey(q), q);
        }

        int inserted = 0, updated = 0, unchanged = 0;

        for (FreightQuote q : parsedMap.values()) {
            String key = buildKey(q);
            FreightQuote old = existingMap.get(key);

            if (old == null) {
                // 新增
                quoteMapper.insert(q);
                inserted++;
            } else if (isDifferent(old, q)) {
                // 有变化，更新
                q.setId(old.getId());
                updateImportedQuote(q);
                updated++;
            } else {
                // 无变化
                unchanged++;
            }
        }

        QuoteUploadLog log2 = new QuoteUploadLog();
        log2.setFileName(fileName);
        log2.setValidFrom(validFrom);
        log2.setValidTo(validTo);
        log2.setTotal(parsed.size());
        log2.setInserted(inserted);
        log2.setUpdated(updated);
        log2.setUnchanged(unchanged);
        uploadLogMapper.insert(log2);

        log.info("报价上传完成: 新增={}, 更新={}, 未变={}", inserted, updated, unchanged);
        return log2;
    }

    @Override
    public IPage<FreightQuote> query(QuoteQueryDTO dto) {
        LambdaQueryWrapper<FreightQuote> w = new LambdaQueryWrapper<FreightQuote>()
            .eq(FreightQuote::getDeleted, 0);

        if (StringUtils.hasText(dto.getCountry())) {
            w.like(FreightQuote::getCountry, dto.getCountry());
        }
        if (StringUtils.hasText(dto.getDestination())) {
            w.like(FreightQuote::getDestination, dto.getDestination());
        }
        // 按体积匹配区间：volumeMin <= dto.volume <= volumeMax (volumeMax为null时无上限)
        if (dto.getVolume() != null) {
            BigDecimal v = dto.getVolume();
            w.and(qw -> qw
                .isNull(FreightQuote::getVolumeMin)
                .or(inner -> inner
                    .le(FreightQuote::getVolumeMin, v)
                    .and(inner2 -> inner2
                        .isNull(FreightQuote::getVolumeMax)
                        .or().ge(FreightQuote::getVolumeMax, v)
                    )
                )
            );
        }
        w.orderByAsc(FreightQuote::getCountry)
         .orderByAsc(FreightQuote::getDestination)
         .orderByAsc(FreightQuote::getVolumeMin);

        IPage<FreightQuote> page = quoteMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), w);
        enrichUpcomingSchedules(page.getRecords());
        return page;
    }

    @Override
    public List<String> listCountries() {
        return quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>()
                .select(FreightQuote::getCountry)
                .eq(FreightQuote::getDeleted, 0)
                .isNotNull(FreightQuote::getCountry)
                // 按报价记录进入数据库的顺序展示国家，避免下拉框重新按名称排序
                .orderByAsc(FreightQuote::getId)
        ).stream().map(FreightQuote::getCountry)
         .filter(StringUtils::hasText)
         .distinct()
         .collect(Collectors.toList());
    }

    @Override
    public List<String> listDestinations(String country) {
        LambdaQueryWrapper<FreightQuote> w = new LambdaQueryWrapper<FreightQuote>()
            .select(FreightQuote::getDestination)
            .eq(FreightQuote::getDeleted, 0)
            .groupBy(FreightQuote::getDestination)
            .orderByAsc(FreightQuote::getDestination);
        if (StringUtils.hasText(country)) {
            w.like(FreightQuote::getCountry, country);
        }
        return quoteMapper.selectList(w).stream()
            .map(FreightQuote::getDestination)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<FreightQuote> listAll() {
        return quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>()
                .eq(FreightQuote::getDeleted, 0)
                .orderByAsc(FreightQuote::getCountry)
                .orderByAsc(FreightQuote::getDestination)
                .orderByAsc(FreightQuote::getVolumeMin)
        );
    }

    @Override
    public List<FreightQuote> listByPortCode(String portCode) {
        if (portCode == null || portCode.isBlank()) return List.of();
        LambdaQueryWrapper<FreightQuote> w = new LambdaQueryWrapper<FreightQuote>()
            .eq(FreightQuote::getDeleted, 0)
            .eq(FreightQuote::getPortCode, portCode.trim().toUpperCase())
            .orderByAsc(FreightQuote::getVolumeMin)
            .orderByAsc(FreightQuote::getVia);
        List<FreightQuote> rows = quoteMapper.selectList(w);
        enrichUpcomingSchedules(rows);
        return rows;
    }

    @Override
    public List<FreightQuote> listByDestination(String destination) {
        if (destination == null || destination.isBlank()) return List.of();
        LambdaQueryWrapper<FreightQuote> w = new LambdaQueryWrapper<FreightQuote>()
            .eq(FreightQuote::getDeleted, 0)
            .like(FreightQuote::getDestination, destination)
            .orderByAsc(FreightQuote::getVolumeMin)
            .orderByAsc(FreightQuote::getVia);
        List<FreightQuote> rows = quoteMapper.selectList(w);
        enrichUpcomingSchedules(rows);
        return rows;
    }

    @Override
    public void createQuote(FreightQuote quote) {
        if (quote.getSourceSheet() == null || quote.getSourceSheet().isBlank()) {
            quote.setSourceSheet("手动新增");
        }
        quoteMapper.insert(quote);
    }

    @Override
    public void updateQuote(FreightQuote quote) {
        if (quote.getId() == null) throw new BusinessException("ID不能为空");
        FreightQuote exist = quoteMapper.selectById(quote.getId());
        if (exist == null) throw new BusinessException("报价记录不存在");
        quoteMapper.updateById(quote);
    }

    @Override
    public void deleteQuote(Long id) {
        if (id == null) throw new BusinessException("ID不能为空");
        quoteMapper.deleteById(id);
    }

    @Override
    public List<QuoteUploadLog> uploadLogs() {
        return uploadLogMapper.selectList(
            new LambdaQueryWrapper<QuoteUploadLog>().orderByDesc(QuoteUploadLog::getCreateTime)
        );
    }

    // 构建唯一键
    private static String buildKey(FreightQuote q) {
        return String.join("||",
            nullStr(q.getSourceSheet()),
            nullStr(q.getCountry()),
            nullStr(q.getDestination()),
            nullStr(q.getVolumeRange()),
            nullStr(q.getVia()),
            q.getValidFrom() == null ? "" : q.getValidFrom().toString(),
            q.getValidTo() == null ? "" : q.getValidTo().toString()
        );
    }

    /** 导入是完整快照，必须允许 Excel 空值覆盖数据库旧值。 */
    private void updateImportedQuote(FreightQuote q) {
        UpdateWrapper<FreightQuote> update = new UpdateWrapper<FreightQuote>()
            .eq("id", q.getId())
            .eq("deleted", 0)
            .set("source_sheet", q.getSourceSheet())
            .set("country", q.getCountry())
            .set("destination", q.getDestination())
            .set("volume_range", q.getVolumeRange())
            .set("volume_min", q.getVolumeMin())
            .set("volume_max", q.getVolumeMax())
            .set("via", q.getVia())
            .set("min_charge", q.getMinCharge())
            .set("of_wuchong", q.getOfWuchong())
            .set("wuchong_first_leg", q.getWuchongFirstLeg())
            .set("wuchong_mother_vessel", q.getWuchongMotherVessel())
            .set("of_jiaoxin", q.getOfJiaoxin())
            .set("jiaoxin_first_leg", q.getJiaoxinFirstLeg())
            .set("jiaoxin_mother_vessel", q.getJiaoxinMotherVessel())
            .set("transit_time", q.getTransitTime())
            .set("cc", q.getCc())
            .set("carrier", q.getCarrier())
            .set("remarks", q.getRemarks())
            .set("port_code", q.getPortCode())
            .set("valid_from", q.getValidFrom())
            .set("valid_to", q.getValidTo());
        quoteMapper.update(null, update);
    }

    // 判断两条记录是否有实质性变化
    private boolean isDifferent(FreightQuote old, FreightQuote neo) {
        return !Objects.equals(old.getOfWuchong(), neo.getOfWuchong())
            || !Objects.equals(old.getOfJiaoxin(), neo.getOfJiaoxin())
            || !Objects.equals(old.getMinCharge(), neo.getMinCharge())
            || !Objects.equals(old.getTransitTime(), neo.getTransitTime())
            || !Objects.equals(old.getCc(), neo.getCc())
            || !Objects.equals(old.getCarrier(), neo.getCarrier())
            || !Objects.equals(old.getValidFrom(), neo.getValidFrom())
            || !Objects.equals(old.getValidTo(), neo.getValidTo());
    }

    private static String nullStr(String s) {
        return s == null ? "" : s;
    }

    private void enrichUpcomingSchedules(List<FreightQuote> quotes) {
        if (quotes == null || quotes.isEmpty()) return;
        Map<String, List<FreightVesselSchedule>> byCode = vesselScheduleService
            .listUpcomingByPortCodes(quotes.stream().map(this::quotePortCode).toList(), 5)
            .stream().collect(Collectors.groupingBy(FreightVesselSchedule::getPortCode));
        for (FreightQuote quote : quotes) {
            String code = VesselScheduleExcelParser.canonicalPortCode(quotePortCode(quote));
            List<FreightVesselSchedule> schedules = byCode.getOrDefault(code, List.of());
            quote.setUpcomingSchedules(schedules);
            quote.setUpcomingScheduleText(schedules.stream()
                .map(this::formatSchedule)
                .collect(Collectors.joining("、")));
        }
    }

    private String quotePortCode(FreightQuote quote) {
        if (StringUtils.hasText(quote.getPortCode())) return quote.getPortCode();
        return QuoteExcelParser.resolvePortCode(quote.getDestination());
    }

    private String formatSchedule(FreightVesselSchedule schedule) {
        String vessel = schedule.getVesselVoyage();
        if (schedule.getEtd() == null) return vessel;
        return vessel + "（ETD " + schedule.getEtd() + "）";
    }
}
