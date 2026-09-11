package com.freight.service;

import com.freight.vo.ShipLocateSearchVO;
import com.freight.vo.ShipLocateStatusVO;
import com.freight.vo.ShipLocateVO;
import com.freight.vo.ShipPortCallVO;

import java.util.List;

public interface AisStreamService {

    List<ShipLocateSearchVO> search(String keyword);

    ShipLocateVO locate(String mmsi);

    default List<ShipPortCallVO> portCalls(String mmsi, int days) {
        return List.of();
    }

    ShipLocateStatusVO status();
}
