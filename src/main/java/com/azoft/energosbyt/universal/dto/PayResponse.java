package com.azoft.energosbyt.universal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PayResponse extends BasicResponse {
  private String txnId;
}
