package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.dto.CheckRequest;
import com.azoft.energosbyt.universal.dto.CheckResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import com.azoft.energosbyt.universal.service.queue.CcbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CheckService {

  @Autowired
  private CcbService ccbService;

  public CheckResponse process(CheckRequest request) {
    BasePerson personSearch = ccbService.searchPersonByAccount(request.getAccount());
    String personId = personSearch.getSrch_res().getRes().get(0).getId();
    String address = ccbService.getAddress(personId, request.getAccount());

    CheckResponse response = new CheckResponse();
    response.setAddress(address);
    return response;
  }
}
