package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.common.exception.BusinessException;
import com.freight.dto.PortChargeSummaryDTO;
import com.freight.entity.DestPortCharge;
import com.freight.entity.PortChargeUploadLog;
import com.freight.entity.FreightQuote;
import com.freight.mapper.DestPortChargeMapper;
import com.freight.mapper.FreightQuoteMapper;
import com.freight.mapper.PortChargeUploadLogMapper;
import com.freight.service.DestPortChargeService;
import com.freight.util.DestPortChargeParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestPortChargeServiceImpl implements DestPortChargeService {

    private final DestPortChargeMapper chargeMapper;
    private final PortChargeUploadLogMapper uploadLogMapper;
    private final FreightQuoteMapper quoteMapper;

    @Override
    @Transactional
    public PortChargeUploadLog uploadAndParse(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        List<DestPortCharge> parsed;
        try {
            parsed = DestPortChargeParser.parse(file.getInputStream(), fileName);
        } catch (Exception e) {
            log.error("目的港费用解析失败", e);
            throw new BusinessException("文件解析失败: " + e.getMessage());
        }
        if (parsed.isEmpty()) throw new BusinessException("未解析到任何费用数据");

        List<DestPortCharge> existing = chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>().eq(DestPortCharge::getDeleted, 0));
        Map<String, DestPortCharge> existMap = existing.stream()
            .collect(Collectors.toMap(this::buildKey, c -> c, (a, b) -> a));

        // 同批次去重：同一 key 只保留第一条（中文名通常在第一行）
        Map<String, DestPortCharge> deduped = new LinkedHashMap<>();
        for (DestPortCharge c : parsed) {
            deduped.putIfAbsent(buildKey(c), c);
        }

        int inserted = 0, updated = 0, unchanged = 0;
        for (DestPortCharge c : deduped.values()) {
            String key = buildKey(c);
            DestPortCharge old = existMap.get(key);
            if (old == null) {
                chargeMapper.insert(c);
                inserted++;
            } else if (isDifferent(old, c)) {
                c.setId(old.getId());
                chargeMapper.updateById(c);
                updated++;
            } else {
                unchanged++;
            }
        }

        PortChargeUploadLog log2 = new PortChargeUploadLog();
        log2.setFileName(fileName);
        log2.setUploadDate(LocalDate.now());
        log2.setTotal(parsed.size());
        log2.setInserted(inserted);
        log2.setUpdated(updated);
        log2.setUnchanged(unchanged);
        uploadLogMapper.insert(log2);
        return log2;
    }

    @Override
    public PortChargeSummaryDTO calcCharges(String destination, BigDecimal volume, String clientType) {
        // 标准化：窖/滘 统一为 滘，去空格容忍格式差异
        String normalized = destination != null ? destination.replace('窖', '滘') : destination;
        String noSpace = normalized != null ? normalized.replace(" ", "") : null;

        // 优先精确匹配 destination 字段；只有查不到时才用 LIKE 模糊匹配
        List<DestPortCharge> items = chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .eq(DestPortCharge::getDeleted, 0)
                .apply("REPLACE(REPLACE(destination, '窖', '滘'), ' ', '') = {0}", noSpace)
                .orderByAsc(DestPortCharge::getId)
        );

        // 精确匹配无结果 → 多级回退匹配
        if (items.isEmpty() && noSpace != null) {
            items = fuzzyMatchDests(noSpace);
        }

        // 容错：Excel 里 KAOHSIUNG 可能拼成 KAOHSUNG（少了个 I）
        if (items.isEmpty() && noSpace != null && noSpace.contains("KAOHSIUNG")) {
            String alt = noSpace.replace("KAOHSIUNG", "KAOHSUNG");
            items = chargeMapper.selectList(
                new LambdaQueryWrapper<DestPortCharge>()
                    .eq(DestPortCharge::getDeleted, 0)
                    .apply("REPLACE(REPLACE(destination, '窖', '滘'), ' ', '') LIKE CONCAT('%', {0}, '%')", alt)
                    .orderByAsc(DestPortCharge::getId)
            );
        }

        PortChargeSummaryDTO dto = new PortChargeSummaryDTO();
        dto.setDestination(destination);
        dto.setVolume(volume);
        dto.setItems(items);

        boolean isDirect = !"coload".equalsIgnoreCase(clientType);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (DestPortCharge c : items) {
            BigDecimal amount = isDirect ? c.getAmountDirect() : c.getAmountCoload();
            String unit = isDirect ? c.getUnitDirect() : c.getUnitCoload();
            BigDecimal min = isDirect ? c.getMinDirect() : null;
            if (amount == null || c.getCurrency() == null) continue;
            BigDecimal amt = calcAmount(amount, unit, volume, min);
            if (amt == null) continue;
            totals.merge(c.getCurrency(), amt, BigDecimal::add);
        }
        dto.setTotalByCurrency(totals);

        String display = totals.entrySet().stream()
            .map(e -> e.getKey() + " " + e.getValue().setScale(2, RoundingMode.HALF_UP))
            .collect(Collectors.joining(" + "));
        dto.setTotalDisplay(display.isEmpty() ? "—" : display);
        return dto;
    }

    /** 多级回退模糊匹配：先 LIKE 原串，不命中则去括号后用港口名匹配，多候选时选最匹配的 */
    private List<DestPortCharge> fuzzyMatchDests(String noSpace) {
        // 步骤1：LIKE 原串
        List<String> candidates = chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .select(DestPortCharge::getDestination)
                .eq(DestPortCharge::getDeleted, 0)
                .apply("REPLACE(REPLACE(destination, '窖', '滘'), ' ', '') LIKE CONCAT('%', {0}, '%')", noSpace)
                .groupBy(DestPortCharge::getDestination)
        ).stream().map(DestPortCharge::getDestination).distinct().collect(Collectors.toList());

        // 步骤2：LIKE 没命中 → 去掉括号后只用港口名匹配
        if (candidates.isEmpty()) {
            String portCore = normalizeDest(noSpace);
            if (!portCore.isEmpty()) {
                candidates = chargeMapper.selectList(
                    new LambdaQueryWrapper<DestPortCharge>()
                        .select(DestPortCharge::getDestination)
                        .eq(DestPortCharge::getDeleted, 0)
                        .apply("REPLACE(REPLACE(destination, '窖', '滘'), ' ', '') LIKE CONCAT('%', {0}, '%')", portCore)
                        .groupBy(DestPortCharge::getDestination)
                ).stream().map(DestPortCharge::getDestination).distinct().collect(Collectors.toList());
            }
        }

        if (candidates.isEmpty()) return List.of();

        // 只有一个候选 → 直接加载
        if (candidates.size() == 1) {
            return chargeMapper.selectList(
                new LambdaQueryWrapper<DestPortCharge>()
                    .eq(DestPortCharge::getDeleted, 0)
                    .eq(DestPortCharge::getDestination, candidates.get(0))
                    .orderByAsc(DestPortCharge::getId)
            );
        }

        // 多个候选 → 按优先级选最匹配的
        // 优先级: 1.港口名相等 2.不含via 3.首个
        final String portCore = normalizeDest(noSpace);
        final List<String> finalCandidates = candidates;
        String best = null;
        // 优先级1：去掉括号后港口名完全相等
        for (String d : finalCandidates) {
            if (d != null && normalizeDest(d).equals(portCore)) {
                best = d;
                break;
            }
        }
        // 优先级2：不含 via（避免 AQABA via DUBAI 抢 DUBAI）
        if (best == null) {
            for (String d : finalCandidates) {
                if (d != null && !d.toLowerCase().contains("via ")) {
                    best = d;
                    break;
                }
            }
        }
        if (best == null) best = finalCandidates.get(0);

        return chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .eq(DestPortCharge::getDeleted, 0)
                .eq(DestPortCharge::getDestination, best)
                .orderByAsc(DestPortCharge::getId)
        );
    }

    private BigDecimal calcAmount(BigDecimal amount, String unit, BigDecimal volume, BigDecimal min) {
        if (amount == null) return null;
        BigDecimal v = volume != null ? volume : BigDecimal.ONE;
        BigDecimal result = switch (unit == null ? "" : unit) {
            case "WM", "RT" -> {
                BigDecimal calc = amount.multiply(v).setScale(2, RoundingMode.HALF_UP);
                yield (min != null && calc.compareTo(min) < 0) ? min : calc;
            }
            case "TON" -> amount.multiply(v).setScale(2, RoundingMode.HALF_UP);
            case "BLOCK" -> {
                long blocks = (long) Math.ceil(v.doubleValue() / 3.0);
                yield amount.multiply(BigDecimal.valueOf(blocks)).setScale(2, RoundingMode.HALF_UP);
            }
            case "100KG" -> {
                BigDecimal hundreds = v.multiply(BigDecimal.valueOf(2.5)).setScale(2, RoundingMode.CEILING);
                yield amount.multiply(hundreds).setScale(2, RoundingMode.HALF_UP);
            }
            default -> amount;
        };
        return result;
    }

    @Override
    @Override
    public List<String> listCountries() {
        // 从报价表取国家列表
        return quoteMapper.selectList(
            new LambdaQueryWrapper<FreightQuote>()
                .select(FreightQuote::getCountry)
                .eq(FreightQuote::getDeleted, 0)
                .isNotNull(FreightQuote::getCountry)
                .groupBy(FreightQuote::getCountry)
                .orderByAsc(FreightQuote::getCountry)
        ).stream().map(FreightQuote::getCountry)
         .filter(StringUtils::hasText).distinct().toList();
    }

    @Override
    public List<String> listDestinations(String country) {
        if (StringUtils.hasText(country)) {
            // 从报价表取该国家下的目的港
            return quoteMapper.selectList(
                new LambdaQueryWrapper<FreightQuote>()
                    .select(FreightQuote::getDestination)
                    .eq(FreightQuote::getDeleted, 0)
                    .like(FreightQuote::getCountry, country)
                    .groupBy(FreightQuote::getDestination)
                    .orderByAsc(FreightQuote::getDestination)
            ).stream().map(FreightQuote::getDestination)
             .filter(StringUtils::hasText).distinct().toList();
        }
        // 无国家筛选时返回全部目的港
        return chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .select(DestPortCharge::getDestination)
                .eq(DestPortCharge::getDeleted, 0)
                .groupBy(DestPortCharge::getDestination)
                .orderByAsc(DestPortCharge::getDestination)
        ).stream().map(DestPortCharge::getDestination)
         .filter(StringUtils::hasText).distinct().toList();
    }

    @Override
    public List<PortChargeUploadLog> uploadLogs() {
        return uploadLogMapper.selectList(
            new LambdaQueryWrapper<PortChargeUploadLog>().orderByDesc(PortChargeUploadLog::getCreateTime));
    }

    /** 标准化目的港：去括号内容、去空格、窖→滘 */
    private static String normalizeDest(String dest) {
        if (dest == null) return null;
        return dest.replace('窖', '滘').replace(" ", "").replaceAll("\\(.*\\)", "").trim();
    }

    private String buildKey(DestPortCharge c) {
        return (c.getDestination() == null ? "" : c.getDestination()) + "||"
            + (c.getFeeNameEn() == null ? "" : c.getFeeNameEn());
    }

    private boolean isDifferent(DestPortCharge old, DestPortCharge neo) {
        return !Objects.equals(old.getAmountDirect(), neo.getAmountDirect())
            || !Objects.equals(old.getUnitDirect(), neo.getUnitDirect())
            || !Objects.equals(old.getCurrency(), neo.getCurrency())
            || !Objects.equals(old.getAmountCoload(), neo.getAmountCoload());
    }

    @Override
    public List<DestPortCharge> listByDestination(String destination) {
        if (destination == null || destination.isBlank()) return List.of();
        String normalized = destination.replace('窖', '滘').replace(" ", "");
        return chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .eq(DestPortCharge::getDeleted, 0)
                .apply("REPLACE(REPLACE(destination, '窖', '滘'), ' ', '') LIKE CONCAT('%', {0}, '%')", normalized)
                .orderByAsc(DestPortCharge::getId)
        );
    }

    @Override
    public void createCharge(DestPortCharge charge) {
        chargeMapper.insert(charge);
    }

    @Override
    public void updateCharge(DestPortCharge charge) {
        if (charge.getId() == null) throw new BusinessException("ID不能为空");
        DestPortCharge exist = chargeMapper.selectById(charge.getId());
        if (exist == null) throw new BusinessException("费用记录不存在");
        chargeMapper.updateById(charge);
    }

    @Override
    public void deleteCharge(Long id) {
        chargeMapper.deleteById(id);
    }

    @Override
    public List<DestPortCharge> listAll() {
        return chargeMapper.selectList(
            new LambdaQueryWrapper<DestPortCharge>()
                .eq(DestPortCharge::getDeleted, 0)
                .orderByAsc(DestPortCharge::getDestination)
                .orderByAsc(DestPortCharge::getId)
        );
    }
}
