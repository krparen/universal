package com.azoft.energosbyt.universal.dto;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@ToString
public class PayRequest {

  private static final String DATE_TIME_FORMAT = "dd.mm.yyyy HH:mm:ss";

  private String system;
  private String account;
  private String txnId;

  @DateTimeFormat(pattern = DATE_TIME_FORMAT) // отвечает за десериализацию
  private LocalDateTime datePay;

  @Data
  public static class Service {
    private String code;
    private String sum;
  }

}
