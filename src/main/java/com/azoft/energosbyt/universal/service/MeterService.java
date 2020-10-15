package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.dto.BaseMeter;
import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.exception.ErrorCode;
import com.azoft.energosbyt.universal.dto.Meter;
import com.azoft.energosbyt.universal.dto.MeterResponse;
import com.azoft.energosbyt.universal.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class MeterService {

    @Value("${energosbyt.rabbit.request.check.queue-name}")
    private String checkRequestQueueName;
    @Value("${energosbyt.rabbit.request.timeout-in-ms}")
    private Long requestTimeout;
    @Value("${energosbyt.application.this-system-id}")
    private String thisSystemId;

    private final AmqpTemplate template;
    private final AmqpAdmin rabbitAdmin;
    private final ObjectMapper mapper;

    public MeterService(AmqpTemplate template, AmqpAdmin rabbitAdmin, ObjectMapper mapper) {
        this.template = template;
        this.rabbitAdmin = rabbitAdmin;
        this.mapper = mapper;
    }

    public MeterResponse process(String system, String account) {

        String personReplyQueueName = null;
        String metersReplyQueueName = null;
        Set<String> openMeterValueReplyQueues = new HashSet<>();

        try {
            personReplyQueueName = declareReplyQueueWithUuidName();
            BasePerson personRabbitResponse = searchPersonByAccount(account, personReplyQueueName);
            String personId = personRabbitResponse.getSrch_res().getRes().get(0).getId();

            metersReplyQueueName = declareReplyQueueWithUuidName();
            BaseMeter metersRabbitResponse = searchMetersByPersonId(metersReplyQueueName, personId);
            log.info("User with id {} has meters {}", personId, metersRabbitResponse.getSrch_res().getServ());

            Map<String, String> activeMetersIdAndServiceType = getActiveMetersIdAndServiceType(metersRabbitResponse);

            List<BaseMeter> activeMeters = getActiveMeters(openMeterValueReplyQueues, activeMetersIdAndServiceType.keySet());
            return getMeterResponse(activeMeters, activeMetersIdAndServiceType);
        } finally {
            if (personReplyQueueName != null) {
                rabbitAdmin.deleteQueue(personReplyQueueName);
            }
            if (metersReplyQueueName != null) {
                rabbitAdmin.deleteQueue(metersReplyQueueName);
            }
            openMeterValueReplyQueues.forEach( queueName -> {
                if (queueName != null) {
                    rabbitAdmin.deleteQueue(queueName);
                }
            });
        }
    }

    private List<BaseMeter> getActiveMeters(Set<String> openMeterValueReplyQueues, Set<String> activeMeterIds) {
        List<BaseMeter> activeMeters = new ArrayList<>();
        activeMeterIds.forEach(id -> {
            String meterValueReplyQueue = declareReplyQueueWithUuidName();
            openMeterValueReplyQueues.add(meterValueReplyQueue);
            BaseMeter activeMeter = getMeterById(meterValueReplyQueue, id);
            activeMeters.add(activeMeter);
            rabbitAdmin.deleteQueue(meterValueReplyQueue);
            openMeterValueReplyQueues.remove(meterValueReplyQueue);
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

    private BaseMeter searchMetersByPersonId(String metersReplyQueueName, String personId) {
        MessageProperties metersMessageProperties = createMetersMessageProperties(metersReplyQueueName);
        byte[] metersMessageBody = createMetersMessageBody(personId);
        Message metersRequestMessage = new Message(metersMessageBody, metersMessageProperties);

        template.send(checkRequestQueueName, metersRequestMessage);

        return receiveMetersResponse(metersReplyQueueName);
    }

    private BaseMeter getMeterById(String meterValuesReplyQueueName, String meterId) {
        MessageProperties metersMessageProperties = createMeterValuesMessageProperties(meterValuesReplyQueueName);
        byte[] metersMessageBody = createMeterValuesMessageBody(meterId);
        Message meterValuesRequestMessage = new Message(metersMessageBody, metersMessageProperties);

        template.send(checkRequestQueueName, meterValuesRequestMessage);

        return receiveMetersResponse(meterValuesReplyQueueName);
    }

    private byte[] createMeterValuesMessageBody(String meterId) {
        String bodyAsString = null;
        try {
            bodyAsString = mapper.writeValueAsString(createMeterValuesRabbitRequest(meterId));
        } catch (JsonProcessingException e) {
            String message = "Rabbit request serialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }
        log.info("body as String: {}", bodyAsString);

        return bodyAsString.getBytes(StandardCharsets.UTF_8);
    }

    private BaseMeter createMeterValuesRabbitRequest(String meterId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);
        rabbitRequest.setId(meterId);
        return rabbitRequest;
    }

    private MessageProperties createMeterValuesMessageProperties(String meterValuesReplyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "getMeter");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", meterValuesReplyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }

    private BasePerson searchPersonByAccount(String account, String personReplyQueueName) {
        MessageProperties personMessageProperties = createPersonMessageProperties(personReplyQueueName);
        byte[] personMessageBody = createPersonMessageBody(account);
        Message personRequestMessage = new Message(personMessageBody, personMessageProperties);

        template.send(checkRequestQueueName, personRequestMessage);
        BasePerson personRabbitResponse = receivePersonResponse(personReplyQueueName);

        if (personRabbitResponse.getSrch_res().getRes().isEmpty()) {
            String message = "No person found for account id = " + account;
            log.error(message);
            throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR, true);
        }
        return personRabbitResponse;
    }

    private BaseMeter receiveMetersResponse(String replyQueueName) {
        Message responseMessage = safelyReceiveResponse(replyQueueName);
        String responseAsString = getMessageBodyAsString(responseMessage);
        return safelyDeserializeMeterFromResponse(responseAsString);
    }

    private BaseMeter safelyDeserializeMeterFromResponse(String responseAsString) {
        BaseMeter response = null;
        try {
            response = mapper.readValue(responseAsString, BaseMeter.class);
        } catch (JsonProcessingException e) {
            String message = "Rabbit response deserialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }

        log.info("response from rabbit: {}", response);
        return response;
    }

    private MessageProperties createMetersMessageProperties(String metersReplyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "searchMeter");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", metersReplyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }

    private byte[] createMetersMessageBody(String personId) {
        String bodyAsString = null;
        try {
            bodyAsString = mapper.writeValueAsString(createMetersRabbitRequest(personId));
        } catch (JsonProcessingException e) {
            String message = "Rabbit request serialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }
        log.info("body as String: {}", bodyAsString);

        return bodyAsString.getBytes(StandardCharsets.UTF_8);
    }

    private BaseMeter createMetersRabbitRequest(String personId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);

        BaseMeter.Srch search = new BaseMeter.Srch();
        search.setPerson_Id(personId);
        rabbitRequest.setSrch(search);
        return rabbitRequest;
    }

    private String declareReplyQueueWithUuidName() {
        String replyQueueName = UUID.randomUUID().toString();
        Queue newQueue = new Queue(replyQueueName, false, false, true);

        try {
            return rabbitAdmin.declareQueue(newQueue);
        } catch (AmqpException e) {
            String message = "Queue declaration failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }

    }

    private MessageProperties createPersonMessageProperties(String replyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "searchPerson");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", replyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }

    private byte[] createPersonMessageBody(String account) {

        String bodyAsString = null;
        try {
            bodyAsString = mapper.writeValueAsString(createPersonRabbitRequest(account));
        } catch (JsonProcessingException e) {
            String message = "Rabbit request serialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }
        log.info("body as String: {}", bodyAsString);

        return bodyAsString.getBytes(StandardCharsets.UTF_8);
    }

    private BasePerson createPersonRabbitRequest(String account) {
        BasePerson rabbitRequest = new BasePerson();
        rabbitRequest.setSystem_id(thisSystemId);

        BasePerson.Srch search = new BasePerson.Srch();
        search.setAccount_number(account);
        search.setDept("ORESB");
        rabbitRequest.setSrch(search);
        return rabbitRequest;
    }

    private BasePerson receivePersonResponse(String replyQueueName) {
        Message responseMessage = safelyReceiveResponse(replyQueueName);
        String responseAsString = getMessageBodyAsString(responseMessage);
        return safelyDeserializePersonFromResponse(responseAsString);
    }

    private String getMessageBodyAsString(Message responseMessage) {
        String responseAsString = null;
        try {
            responseAsString = new String(responseMessage.getBody(), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            String message = "Unsupported encoding for incoming rabbit message";
            log.error(message + "; rabbit message: {}", responseMessage);
            throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR);
        }
        return responseAsString;
    }

    private Message safelyReceiveResponse(String replyQueueName) {
        Message responseMessage = null;
        try {
            responseMessage = template.receive(replyQueueName, requestTimeout);
        } catch (AmqpException e) {
            String message = "Getting a rabbit response from queue " + replyQueueName + " failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }

        if (responseMessage == null) {
            String message = "Rabbit response from queue " + replyQueueName + " failed timeout";
            log.error(message, replyQueueName);
            throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR);
        }
        return responseMessage;
    }

    private BasePerson safelyDeserializePersonFromResponse(String responseAsString) {
        BasePerson response = null;
        try {
            response = mapper.readValue(responseAsString, BasePerson.class);
        } catch (JsonProcessingException e) {
            String message = "Rabbit response deserialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }

        log.info("response from rabbit: {}", response);
        return response;
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
