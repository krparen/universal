package com.azoft.energosbyt.universal.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CheckRequest {
  private String system;
  private String account;
  private String txnId;
  private BigDecimal sum;
  private List<Service> services;

  @Data
  static class Service {
    private String code;
    private BigDecimal sum;
  }
}
