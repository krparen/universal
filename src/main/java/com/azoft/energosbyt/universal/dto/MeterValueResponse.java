package com.azoft.energosbyt.universal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MeterValueResponse extends BasicResponse {
  @JsonProperty("trx_id")
  private String trxId;
}
