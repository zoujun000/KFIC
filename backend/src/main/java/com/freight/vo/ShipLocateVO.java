package com.freight.vo;

import lombok.Data;

import java.util.List;

@Data
public class ShipLocateVO {

    private ShipInfo ship;
    private LatestPosition latest;
    private List<TrackPoint> track;
    private long updateAt;

    @Data
    public static class ShipInfo {
        private String mmsi;
        private String name;
        private String imo;
        private String callsign;
        private String shipType;
        private Double length;
        private Double breadth;
        private Double draught;
        private String eta;
        private String destination;
    }

    @Data
    public static class LatestPosition {
        private Double lat;
        private Double lon;
        private Double sog;
        private Double cog;
        private Double heading;
        private Integer navStatus;
        private Long time;
    }

    public record TrackPoint(Double lat, Double lon, Long time, Double sog, Double cog) {
    }
}
