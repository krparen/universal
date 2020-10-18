package com.azoft.energosbyt.universal.dto;

import lombok.Data;

@Data
public class MeterValue {
    private String meterId;
    private String meterNumber;
    private String T1;
    private String T2;
    private String T3;
}
