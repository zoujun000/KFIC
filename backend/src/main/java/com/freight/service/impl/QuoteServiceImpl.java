package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freight.common.exception.BusinessException;
import com.freight.dto.QuoteQueryDTO;
import com.freight.entity.FreightQuote;
import com.freight.entity.QuoteUploadLog;
import com.freight.mapper.FreightQuoteMapper;
import com.freight.mapper.QuoteUploadLogMapper;
import com.freight.service.QuoteService;
import com.freight.util.QuoteExcelParser;
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

        // 查出数据库中已有记录（同有效期内的）
        List<FreightQuote> existing = quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>().eq(FreightQuote::getDeleted, 0)
        );

        // 建立唯一key → 已有记录的 Map
        // key = sourceSheet + destination + volumeRange + via
        Map<String, FreightQuote> existingMap = existing.stream()
            .collect(Collectors.toMap(
                QuoteServiceImpl::buildKey,
                q -> q,
                (a, b) -> a  // 有重复取第一个
            ));

        int inserted = 0, updated = 0, unchanged = 0;

        for (FreightQuote q : parsed) {
            String key = buildKey(q);
            FreightQuote old = existingMap.get(key);

            if (old == null) {
                // 新增
                quoteMapper.insert(q);
                inserted++;
            } else if (isDifferent(old, q)) {
                // 有变化，更新
                q.setId(old.getId());
                quoteMapper.updateById(q);
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

        return quoteMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), w);
    }

    @Override
    public List<String> listCountries() {
        return quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>()
                .select(FreightQuote::getCountry)
                .eq(FreightQuote::getDeleted, 0)
                .isNotNull(FreightQuote::getCountry)
                .groupBy(FreightQuote::getCountry)
                .orderByAsc(FreightQuote::getCountry)
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
    public List<QuoteUploadLog> uploadLogs() {
        return uploadLogMapper.selectList(
            new LambdaQueryWrapper<QuoteUploadLog>().orderByDesc(QuoteUploadLog::getCreateTime)
        );
    }

    // 构建唯一键
    private static String buildKey(FreightQuote q) {
        return String.join("||",
            nullStr(q.getSourceSheet()),
            nullStr(q.getDestination()),
            nullStr(q.getVolumeRange()),
            nullStr(q.getVia())
        );
    }

    // 判断两条记录是否有实质性变化
    private boolean isDifferent(FreightQuote old, FreightQuote neo) {
        return !Objects.equals(old.getOfWuchong(), neo.getOfWuchong())
            || !Objects.equals(old.getOfBeisha(), neo.getOfBeisha())
            || !Objects.equals(old.getOfJiaoxin(), neo.getOfJiaoxin())
            || !Objects.equals(old.getMinCharge(), neo.getMinCharge())
            || !Objects.equals(old.getTransitTime(), neo.getTransitTime())
            || !Objects.equals(old.getCarrier(), neo.getCarrier())
            || !Objects.equals(old.getValidFrom(), neo.getValidFrom())
            || !Objects.equals(old.getValidTo(), neo.getValidTo());
    }

    private static String nullStr(String s) {
        return s == null ? "" : s;
    }
}
