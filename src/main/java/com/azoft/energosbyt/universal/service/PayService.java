package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.OperationStatus;
import com.azoft.energosbyt.universal.dto.PayRequest;
import com.azoft.energosbyt.universal.dto.PayResponse;
import org.springframework.stereotype.Service;

@Service
public class PayService {

  public PayResponse process(PayRequest payRequest) {
    PayResponse response = new PayResponse();
    response.setStatus(OperationStatus.ok);
    response.setTxnId(payRequest.getTxnId());
    return response;
  }
}
