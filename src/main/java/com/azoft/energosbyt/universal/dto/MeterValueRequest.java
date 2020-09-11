package com.azoft.energosbyt.universal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class MeterValueRequest {

  private static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

  private String system;
  private String account;
  private String txnId;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT, timezone = "Europe/Moscow")
  private LocalDateTime dateMv;

  private List<Meter> meterValues = new ArrayList<>();
}
