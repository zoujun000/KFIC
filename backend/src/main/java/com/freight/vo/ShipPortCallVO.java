package com.freight.vo;

import lombok.Data;

@Data
public class ShipPortCallVO {

    private String portName;
    private String portCode;
    private String countryName;
    private String terminalName;
    private String berthName;
    private String arrivalAnchorage;
    private String ata;
    private String atb;
    private String atd;
    private Double stayTime;
    private Double stayTerminalTime;
    private Double arrivalDraught;
    private Double departureDraught;
}
