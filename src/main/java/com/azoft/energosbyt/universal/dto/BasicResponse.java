package com.azoft.energosbyt.universal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class BasicResponse {
  private OperationStatus status;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer errorCode;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String errorMessage;
}
