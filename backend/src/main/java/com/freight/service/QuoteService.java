package com.freight.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.freight.dto.QuoteQueryDTO;
import com.freight.entity.FreightQuote;
import com.freight.entity.QuoteUploadLog;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuoteService {

    /**
     * 上传并解析报价Excel，与数据库对比后增量更新
     */
    QuoteUploadLog uploadAndParse(MultipartFile file);

    /**
     * 根据国家/目的地/体积查询报价
     */
    IPage<FreightQuote> query(QuoteQueryDTO dto);

    /**
     * 获取所有国家列表（用于下拉）
     */
    List<String> listCountries();

    /**
     * 获取目的港列表
     */
    List<String> listDestinations(String country);

    /**
     * 上传历史记录
     */
    List<QuoteUploadLog> uploadLogs();
}
