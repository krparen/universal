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

  private static final String TXN_RECORD_WITH_SAME_ID_EXISTS =
          "Transaction with id = %s and system = %s is in progress or finished";

  @Autowired
  private CcbService ccbService;
  @Autowired
  private UniversalTxnRepository txnRepository;

  public CheckResponse process(CheckRequest request) {

    if (txnRepository.findByTxnIdAndSystem(request.getTxnId(), request.getSystem()) != null) {
      String message = String.format(TXN_RECORD_WITH_SAME_ID_EXISTS, request.getTxnId(), request.getSystem());
      log.error(message);
      throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR, true);
    }

    BasePerson personSearch = ccbService.searchPersonByAccount(request.getAccount());
    String personId = personSearch.getSrch_res().getRes().get(0).getId();
    String address = ccbService.getAddress(personId, request.getAccount());

    CheckResponse response = new CheckResponse();
    response.setAddress(address);
    return response;
  }
}
