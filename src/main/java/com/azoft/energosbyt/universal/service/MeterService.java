package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.BaseMeter;
import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.dto.Meter;
import com.azoft.energosbyt.universal.dto.MeterResponse;
import com.azoft.energosbyt.universal.service.queue.CcbQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class MeterService {

    private final CcbQueueService ccbQueueService;

    public MeterService(CcbQueueService ccbQueueService) {
        this.ccbQueueService = ccbQueueService;
    }

    public MeterResponse process(String system, String account) {
        BasePerson personRabbitResponse = ccbQueueService.searchPersonByAccount(account);
        String personId = personRabbitResponse.getSrch_res().getRes().get(0).getId();

        BaseMeter metersRabbitResponse = ccbQueueService.searchMetersByPersonId(personId);
        log.info("User with id {} has meters {}", personId, metersRabbitResponse.getSrch_res().getServ());

        Map<String, String> activeMetersIdAndServiceType = getActiveMetersIdAndServiceType(metersRabbitResponse);

        List<BaseMeter> activeMeters = getActiveMeters(activeMetersIdAndServiceType.keySet());
        return getMeterResponse(activeMeters, activeMetersIdAndServiceType);
    }

    private List<BaseMeter> getActiveMeters(Set<String> activeMeterIds) {
        List<BaseMeter> activeMeters = new ArrayList<>();
        activeMeterIds.forEach(id -> {
            BaseMeter activeMeter = ccbQueueService.getMeterById(id);
            activeMeters.add(activeMeter);
        });
        return activeMeters;
    }

    private Map<String, String> getActiveMetersIdAndServiceType(BaseMeter metersSearchResult) {
        Map<String, String> activeMetersIdAndServiceType = new HashMap<>();

        List<BaseMeter.Srch_res.Srch_res_s> services = metersSearchResult.getSrch_res().getServ();
        for (BaseMeter.Srch_res.Srch_res_s service : services) {
            List<BaseMeter.Srch_res.Srch_res_s.Service_point> servicePoints = service.getSPs();
            for (BaseMeter.Srch_res.Srch_res_s.Service_point servicePoint : servicePoints) {
                List<BaseMeter.Srch_res.Srch_res_s.Service_point.Conn_history> connectionHistories = servicePoint.getCHs();
                boolean isOpenConnectionExists = false;
                for (BaseMeter.Srch_res.Srch_res_s.Service_point.Conn_history connectionHistory : connectionHistories) {
                    if (connectionHistory.getStop_date() == null) {
                        isOpenConnectionExists = true;
                        break;
                    }
                }

                if (isOpenConnectionExists) {
                    List<BaseMeter.Srch_res.Srch_res_s.Service_point.Sp_history> spHistories = servicePoint.getSPHs();
                    for (BaseMeter.Srch_res.Srch_res_s.Service_point.Sp_history spHistory : spHistories) {
                        if (spHistory.getRemove_date() == null) {
                            activeMetersIdAndServiceType.put(spHistory.getMeter_id(), servicePoint.getServiceType());
                        }
                    }
                }
            }
        }

        return activeMetersIdAndServiceType;
    }

    private MeterResponse getMeterResponse(List<BaseMeter> activeMeters, Map<String, String> activeMetersIdAndServiceType) {

        List<Meter> meters = new ArrayList<>();

        activeMeters.forEach(baseMeter -> {
            Meter meter = new Meter();
            meter.setMeterId(baseMeter.getId());
            meter.setMeterNumber(baseMeter.getBadgeNumber());
            meter.setServiceName(activeMetersIdAndServiceType.get(baseMeter.getId()));

            List<BaseMeter.Registr> registrs = baseMeter.getRegisters();

            if (!registrs.isEmpty()) {
                meter.setDigits(registrs.get(0).getNumberOfDigitsLeft());
            }

            Map<String, String> meterData = new HashMap<>();
            meterData.put("T1", baseMeter.getNumber());
            meter.setMeterData(meterData);

            meters.add(meter);
        });

        MeterResponse response = new MeterResponse();
        response.setMeters(meters);

        return response;
    }

}
