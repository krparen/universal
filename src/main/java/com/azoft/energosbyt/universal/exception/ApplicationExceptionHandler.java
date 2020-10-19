package com.azoft.energosbyt.universal.exception;
import com.azoft.energosbyt.universal.dto.BasicResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApplicationExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<BasicResponse> handleApiException(ApiException exception) {
    BasicResponse response = new BasicResponse();
    response.setStatus(OperationStatus.error);
    response.setErrorCode(exception.getErrorCode().getStringValue());
    if (exception.isUseMessageAsComment()) {
      response.setErrorMessage(exception.getMessage());
    }

    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }
}
