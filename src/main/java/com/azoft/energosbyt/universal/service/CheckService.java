package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.dto.CheckRequest;
import com.azoft.energosbyt.universal.dto.CheckResponse;
import com.azoft.energosbyt.universal.exception.ApiException;
import com.azoft.energosbyt.universal.exception.ErrorCode;
import com.azoft.energosbyt.universal.repository.UniversalTxnRepository;
import com.azoft.energosbyt.universal.service.queue.CcbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CheckService {

  @Autowired
  private CcbService ccbService;
  @Autowired
  private UniversalTxnService txnService;

  public CheckResponse process(CheckRequest request) {

    txnService.assertSameTxnNotExist(request.getTxnId(), request.getSystem());

    BasePerson personSearch = ccbService.searchPersonByAccount(request.getAccount());
    String personId = personSearch.getSrch_res().getRes().get(0).getId();
    String address = ccbService.getAddress(personId, request.getAccount());

    CheckResponse response = new CheckResponse();
    response.setAddress(address);
    return response;
  }
}
