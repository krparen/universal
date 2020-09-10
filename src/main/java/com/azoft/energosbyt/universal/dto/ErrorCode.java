package com.azoft.energosbyt.universal.dto;

public enum ErrorCode {
  INCORRECT_SYSTEM_CODE(1),
  ACCOUNT_NOT_FOUND(2),
  AMOUNT_CANNOT_BE_ACCEPTED(3),
  TXN_ID_NOT_UNIQUE(4),
  UNEXPECTED_ERROR(10);

  private int numericValue;

  ErrorCode(int numericValue) {
    this.numericValue = numericValue;
  }
}
