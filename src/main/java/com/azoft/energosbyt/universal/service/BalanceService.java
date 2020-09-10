package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.BalanceResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

  public BalanceResponse process(String system, String account) {
    BalanceResponse response = new BalanceResponse();
    response.setStatus(OperationStatus.ok);

    BalanceResponse.Service svc1 = new BalanceResponse.Service();
    svc1.setName("Интернет телевидение");
    svc1.setCode("IPTV");
    BigDecimal balance = BigDecimal.valueOf(11.22);
    svc1.setBalance(balance);

    BalanceResponse.Service svc2 = new BalanceResponse.Service();
    svc2.setName("Уборка дома");
    svc2.setCode("СLEANING_HOME");
    BigDecimal balance2 = BigDecimal.valueOf(22.98);
    svc2.setBalance(balance2);

    List<BalanceResponse.Service> services = new ArrayList<>();
    services.add(svc1);
    services.add(svc2);

    response.setServices(services);
    return response;
  }
}
