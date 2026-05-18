package com.freight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freight.entity.PortChargeUploadLog;
import com.freight.entity.QuoteUploadLog;
import com.freight.mapper.PortChargeUploadLogMapper;
import com.freight.mapper.QuoteUploadLogMapper;
import com.freight.service.LogService;
import com.freight.vo.LogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final QuoteUploadLogMapper quoteUploadLogMapper;
    private final PortChargeUploadLogMapper portChargeUploadLogMapper;

    @Override
    public List<LogVO> listAll() {
        return mergeAndSort(
            fetchQuoteLogs(),
            fetchPortChargeLogs()
        );
    }

    @Override
    public List<LogVO> listByType(String type) {
        if ("QUOTE_UPLOAD".equals(type)) {
            return fetchQuoteLogs();
        } else if ("PORT_CHARGE_UPLOAD".equals(type)) {
            return fetchPortChargeLogs();
        }
        return listAll();
    }

    private List<LogVO> fetchQuoteLogs() {
        List<QuoteUploadLog> logs = quoteUploadLogMapper.selectList(
            new LambdaQueryWrapper<QuoteUploadLog>()
                .orderByDesc(QuoteUploadLog::getCreateTime)
        );
        return logs.stream().map(l -> {
            LogVO vo = new LogVO();
            vo.setId(l.getId());
            vo.setType("QUOTE_UPLOAD");
            vo.setTypeName("散货报价上传");
            vo.setFileName(l.getFileName());
            vo.setTotal(l.getTotal());
            vo.setInserted(l.getInserted());
            vo.setUpdated(l.getUpdated());
            vo.setUnchanged(l.getUnchanged());
            vo.setValidFrom(l.getValidFrom());
            vo.setValidTo(l.getValidTo());
            vo.setCreateTime(l.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<LogVO> fetchPortChargeLogs() {
        List<PortChargeUploadLog> logs = portChargeUploadLogMapper.selectList(
            new LambdaQueryWrapper<PortChargeUploadLog>()
                .orderByDesc(PortChargeUploadLog::getCreateTime)
        );
        return logs.stream().map(l -> {
            LogVO vo = new LogVO();
            vo.setId(l.getId());
            vo.setType("PORT_CHARGE_UPLOAD");
            vo.setTypeName("目的港费用上传");
            vo.setFileName(l.getFileName());
            vo.setTotal(l.getTotal());
            vo.setInserted(l.getInserted());
            vo.setUpdated(l.getUpdated());
            vo.setUnchanged(l.getUnchanged());
            vo.setCreateTime(l.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<LogVO> mergeAndSort(List<LogVO> a, List<LogVO> b) {
        return Stream.concat(a.stream(), b.stream())
            .sorted(Comparator.comparing(LogVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
    }
}
