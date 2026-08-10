package com.freight.vo;

import lombok.Data;

@Data
public class ShipLocateStatusVO {

    private boolean connected;
    private long lastMessageAt;
    private int trackedShips;
    private int nameIndexSize;
}
