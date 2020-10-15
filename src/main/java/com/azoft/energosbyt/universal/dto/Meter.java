package com.azoft.energosbyt.universal.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Meter {
  private String meterId;
  private String meterNumber;
  private String serviceName;
  private String digits;
  private Map<String, String> meterData = new HashMap<>();
}
