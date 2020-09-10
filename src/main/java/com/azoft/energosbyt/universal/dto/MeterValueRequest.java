package com.azoft.energosbyt.universal.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class MeterValueRequest {

  private static final String DATE_TIME_FORMAT = "dd.mm.yyyy HH:mm:ss";

  private String system;
  private String account;
  private String txnId;
  @DateTimeFormat(pattern = DATE_TIME_FORMAT) // отвечает за десериализацию
  private LocalDateTime dateMv;

  private List<Meter> meterValues = new ArrayList<>();
}
