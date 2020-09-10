package com.azoft.energosbyt.universal.controller;

import com.azoft.energosbyt.universal.dto.CheckRequest;
import com.azoft.energosbyt.universal.dto.CheckResponse;
import com.azoft.energosbyt.universal.service.CheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController(UniversalController.API_URL)
@Slf4j
@RequiredArgsConstructor
public class UniversalController {

  public static final String API_URL = "/api";
  public static final String CHECK_URL = "check";
  public static final String BALANCE_URL = "balance";
  public static final String PAY_URL = "pay";
  public static final String METERS_URL = "meter";
  public static final String METERS_VALUE_URL = "meter/value";

  private final CheckService checkService;

  @PostMapping(CHECK_URL)
  public CheckResponse check(@RequestBody CheckRequest request) {
    return checkService.process(request);
  }


}
