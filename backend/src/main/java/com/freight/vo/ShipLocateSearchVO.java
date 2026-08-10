package com.freight.vo;

import lombok.Data;

@Data
public class ShipLocateSearchVO {

    private String mmsi;
    private String name;
    private String imo;
    private String callsign;
    private String shipType;
    private Double lat;
    private Double lon;
    private Long lastUpdateAt;
}
