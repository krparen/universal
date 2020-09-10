package com.azoft.energosbyt.universal.controller;

import com.azoft.energosbyt.universal.dto.BalanceResponse;
import com.azoft.energosbyt.universal.dto.CheckRequest;
import com.azoft.energosbyt.universal.dto.CheckResponse;
import com.azoft.energosbyt.universal.dto.PayRequest;
import com.azoft.energosbyt.universal.dto.PayResponse;
import com.azoft.energosbyt.universal.service.BalanceService;
import com.azoft.energosbyt.universal.service.CheckService;
import com.azoft.energosbyt.universal.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
  private final BalanceService balanceService;
  private final PayService payService;

  @PostMapping(CHECK_URL)
  public CheckResponse check(@RequestBody CheckRequest request) {
    return checkService.process(request);
  }

  @GetMapping(BALANCE_URL)
  public BalanceResponse balance(@RequestParam String system, @RequestParam String account) {
    return balanceService.process(system, account);
  }

  @PostMapping(PAY_URL)
  public PayResponse pay(@RequestBody PayRequest request) {
    return payService.process(request);
  }
}
