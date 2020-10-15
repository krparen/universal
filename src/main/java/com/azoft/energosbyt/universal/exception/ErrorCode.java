package com.azoft.energosbyt.universal.exception;

import lombok.Getter;

public enum ErrorCode {
  INCORRECT_SYSTEM_CODE(1),
  ACCOUNT_NOT_FOUND(2),
  AMOUNT_CANNOT_BE_ACCEPTED(3),
  TXN_ID_NOT_UNIQUE(4),
  METER_NOT_FOUND(5),
  METER_DATA_PROCESSING_ERROR(6),
  UNEXPECTED_ERROR(10);

  @Getter
  private int numericValue;

  ErrorCode(int numericValue) {
    this.numericValue = numericValue;
  }
}
