package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.MeterValueRequest;
import com.azoft.energosbyt.universal.dto.MeterValueResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import org.springframework.stereotype.Service;

@Service
public class MeterValueService {

  public MeterValueResponse process(MeterValueRequest meterValueRequest) {
    MeterValueResponse response = new MeterValueResponse();
    response.setStatus(OperationStatus.ok);
    response.setTxnId(meterValueRequest.getTxnId());
    return response;
  }
}
