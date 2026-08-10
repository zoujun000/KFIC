package com.freight.service;

import com.freight.vo.ShipLocateSearchVO;
import com.freight.vo.ShipLocateStatusVO;
import com.freight.vo.ShipLocateVO;

import java.util.List;

public interface AisStreamService {

    List<ShipLocateSearchVO> search(String keyword);

    ShipLocateVO locate(String mmsi);

    ShipLocateStatusVO status();
}
