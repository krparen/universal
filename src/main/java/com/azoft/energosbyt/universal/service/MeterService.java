package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.Meter;
import com.azoft.energosbyt.universal.dto.MeterResponse;
import com.azoft.energosbyt.universal.dto.OperationStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MeterService {

  public MeterResponse process(String system, String account) {
    MeterResponse response = new MeterResponse();
    response.setStatus(OperationStatus.ok);

    Meter meter1 = new Meter();
    meter1.setMeterId(UUID.randomUUID().toString());
    meter1.setMeterNumber("35A136Z");
    meter1.setDigits(6);
    meter1.setServiceName("Горячая вода");

    Map<String, Long> meter1data = new HashMap<>();
    meter1data.put("day", 3313L);
    meter1data.put("night", 31351L);

    meter1.setMeterData(meter1data);

    Meter meter2 = new Meter();
    meter2.setMeterId(UUID.randomUUID().toString());
    meter2.setMeterNumber("AAA3351");
    meter2.setDigits(6);
    meter2.setServiceName("Холодная вода");

    Map<String, Long> meter2data = new HashMap<>();
    meter2data.put("day", 33133L);
    meter2data.put("night", 313515L);

    meter2.setMeterData(meter2data);

    List<Meter> meters = new ArrayList<>();
    meters.add(meter1);
    meters.add(meter2);

    response.setMeters(meters);
    return response;
  }
}
