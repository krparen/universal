package com.azoft.energosbyt.universal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PayRequest {

  private static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

  private String system;
  private String account;
  private String txnId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT, timezone = "Europe/Moscow")
  private LocalDateTime datePay;

  @Data
  public static class Service {
    private String code;
    private String sum;
  }

}
