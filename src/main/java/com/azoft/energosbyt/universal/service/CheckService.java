package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.CheckRequest;
import com.azoft.energosbyt.universal.dto.CheckResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import org.springframework.stereotype.Service;

@Service
public class CheckService {

  public CheckResponse process(CheckRequest request) {
    CheckResponse response = new CheckResponse();
    response.setStatus(OperationStatus.ok);
    response.setAddress("г. Тестовый, улица Тестовый, дом Т1, кв. -346");
    return response;
  }
}
