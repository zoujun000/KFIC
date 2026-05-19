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
     * 查询全部报价（无过滤无分页）
     */
    List<FreightQuote> listAll();

    /**
     * 按港口缩写查询报价
     */
    List<FreightQuote> listByPortCode(String portCode);

    /**
     * 按目的港查询报价列表（无分页，用于管理页）
     */
    List<FreightQuote> listByDestination(String destination);

    /**
     * 更新单条报价
     */
    void updateQuote(FreightQuote quote);

    /**
     * 删除单条报价（逻辑删除）
     */
    void deleteQuote(Long id);

    /**
     * 上传历史记录
     */
    List<QuoteUploadLog> uploadLogs();
}
