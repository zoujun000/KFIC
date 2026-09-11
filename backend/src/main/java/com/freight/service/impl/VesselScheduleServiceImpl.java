package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.freight.common.exception.BusinessException;
import com.freight.entity.FreightVesselSchedule;
import com.freight.entity.VesselScheduleUploadLog;
import com.freight.mapper.FreightVesselScheduleMapper;
import com.freight.mapper.VesselScheduleUploadLogMapper;
import com.freight.service.VesselScheduleService;
import com.freight.util.VesselScheduleExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VesselScheduleServiceImpl implements VesselScheduleService {

    private final FreightVesselScheduleMapper scheduleMapper;
    private final VesselScheduleUploadLogMapper uploadLogMapper;

    @Override
    @Transactional
    public VesselScheduleUploadLog uploadAndParse(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择船期 Excel 文件");
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                && !fileName.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new BusinessException("船期文件仅支持 .xlsx 或 .xls");
        }

        VesselScheduleExcelParser.ParseResult parsed;
        try {
            parsed = VesselScheduleExcelParser.parse(file.getInputStream(), fileName);
        } catch (Exception e) {
            log.error("大船船期 Excel 解析失败", e);
            throw new BusinessException("Excel解析失败: " + e.getMessage());
        }
        if (parsed.records().isEmpty()) throw new BusinessException("未解析到船期数据，请检查表格是否为 SCHE 格式");

        List<FreightVesselSchedule> existing = scheduleMapper.selectList(
            new LambdaQueryWrapper<FreightVesselSchedule>().eq(FreightVesselSchedule::getSourceFile, fileName));
        Map<String, FreightVesselSchedule> oldMap = existing.stream()
            .collect(Collectors.toMap(this::naturalKey, item -> item, (a, b) -> a));
        Set<String> seen = new HashSet<>();
        int inserted = 0, updated = 0, unchanged = 0;

        for (FreightVesselSchedule row : parsed.records()) {
            String key = naturalKey(row);
            if (!seen.add(key)) continue;
            FreightVesselSchedule old = oldMap.get(key);
            if (old == null) {
                row.setDeleted(0);
                scheduleMapper.insert(row);
                inserted++;
            } else if (isDifferent(old, row) || old.getDeleted() != null && old.getDeleted() != 0) {
                row.setId(old.getId());
                update(row);
                updated++;
            } else {
                unchanged++;
            }
        }

        int removed = 0;
        for (FreightVesselSchedule old : existing) {
            if (!seen.contains(naturalKey(old)) && (old.getDeleted() == null || old.getDeleted() == 0)) {
                scheduleMapper.update(null, new UpdateWrapper<FreightVesselSchedule>()
                    .eq("id", old.getId()).set("deleted", 1));
                removed++;
            }
        }

        VesselScheduleUploadLog logRow = new VesselScheduleUploadLog();
        logRow.setFileName(fileName);
        logRow.setTotal(parsed.records().size());
        logRow.setInserted(inserted);
        logRow.setUpdated(updated);
        logRow.setUnchanged(unchanged);
        logRow.setRemoved(removed);
        logRow.setSkipped(parsed.skipped());
        logRow.setWarnings(parsed.warnings().isEmpty() ? null : String.join("; ", parsed.warnings()));
        uploadLogMapper.insert(logRow);
        return logRow;
    }

    @Override
    public List<FreightVesselSchedule> listUpcomingByPortCodes(Collection<String> portCodes, int perPortLimit) {
        if (portCodes == null || portCodes.isEmpty()) return List.of();
        List<String> codes = portCodes.stream().filter(code -> code != null && !code.isBlank())
            .map(code -> VesselScheduleExcelParser.canonicalPortCode(code)).distinct().toList();
        if (codes.isEmpty()) return List.of();
        List<FreightVesselSchedule> rows = scheduleMapper.selectList(
            new LambdaQueryWrapper<FreightVesselSchedule>()
                .eq(FreightVesselSchedule::getDeleted, 0)
                .in(FreightVesselSchedule::getPortCode, codes)
                .ge(FreightVesselSchedule::getEtd, LocalDate.now())
                .orderByAsc(FreightVesselSchedule::getEtd)
                .orderByAsc(FreightVesselSchedule::getPortCode)
        );
        Map<String, Integer> counts = new HashMap<>();
        return rows.stream().filter(row -> {
            int count = counts.getOrDefault(row.getPortCode(), 0);
            if (count >= Math.max(1, perPortLimit)) return false;
            counts.put(row.getPortCode(), count + 1);
            return true;
        }).toList();
    }

    @Override
    public List<VesselScheduleUploadLog> uploadLogs() {
        return uploadLogMapper.selectList(new LambdaQueryWrapper<VesselScheduleUploadLog>()
            .orderByDesc(VesselScheduleUploadLog::getCreateTime));
    }

    private void update(FreightVesselSchedule row) {
        scheduleMapper.update(null, new UpdateWrapper<FreightVesselSchedule>()
            .eq("id", row.getId())
            .set("source_file", row.getSourceFile())
            .set("source_sheet", row.getSourceSheet())
            .set("section_name", row.getSectionName())
            .set("service_name", row.getServiceName())
            .set("port_code", row.getPortCode())
            .set("raw_port_code", row.getRawPortCode())
            .set("port_name", row.getPortName())
            .set("vessel_voyage", row.getVesselVoyage())
            .set("cfs_closing_date", row.getCfsClosingDate())
            .set("stuffing_date", row.getStuffingDate())
            .set("si_cutoff_date", row.getSiCutoffDate())
            .set("etd", row.getEtd())
            .set("eta", row.getEta())
            .set("status", row.getStatus())
            .set("source_row_no", row.getSourceRowNo())
            .set("deleted", 0));
    }

    private boolean isDifferent(FreightVesselSchedule old, FreightVesselSchedule row) {
        return !java.util.Objects.equals(old.getEta(), row.getEta())
            || !java.util.Objects.equals(old.getCfsClosingDate(), row.getCfsClosingDate())
            || !java.util.Objects.equals(old.getStuffingDate(), row.getStuffingDate())
            || !java.util.Objects.equals(old.getSiCutoffDate(), row.getSiCutoffDate())
            || !java.util.Objects.equals(old.getStatus(), row.getStatus())
            || !java.util.Objects.equals(old.getRawPortCode(), row.getRawPortCode());
    }

    private String naturalKey(FreightVesselSchedule row) {
        return String.join("||", value(row.getSourceSheet()), value(row.getSectionName()),
            value(row.getPortCode()), value(row.getVesselVoyage()),
            row.getEtd() == null ? "" : row.getEtd().toString());
    }

    private String value(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
