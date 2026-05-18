package com.freight.service;

import com.freight.vo.LogVO;

import java.util.List;

public interface LogService {
    /** 查询所有上传日志（合并报价+费用） */
    List<LogVO> listAll();

    /** 按类型筛选 */
    List<LogVO> listByType(String type);
}
