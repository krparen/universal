package com.azoft.energosbyt.universal.service.queue;

import com.azoft.energosbyt.universal.dto.BaseMeter;
import com.azoft.energosbyt.universal.dto.BasePerson;
import com.azoft.energosbyt.universal.exception.ApiException;
import com.azoft.energosbyt.universal.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CcbService extends AbstractQueueService {

    private static final String SEARCH_PERSON_TYPE = "searchPerson";
    private static final String SEARCH_METER_TYPE = "searchMeter";
    private static final String GET_METER_TYPE = "getMeter";

    @Value("${energosbyt.rabbit.request.check.queue-name}")
    private String ccbQueueName;

    public BasePerson searchPersonByAccount(String account) {

        String personReplyQueueName = null;

        try {
            personReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties personMessageProperties = createMessageProperties(personReplyQueueName, SEARCH_PERSON_TYPE);
            BasePerson bodyObject = createPersonRabbitRequest(account);
            byte[] personMessageBody = toJsonToBytes(bodyObject);
            Message personRequestMessage = new Message(personMessageBody, personMessageProperties);

            template.send(ccbQueueName, personRequestMessage);
            BasePerson personRabbitResponse = safelyReceiveResponse(personReplyQueueName, BasePerson.class);

            if (personRabbitResponse.getSrch_res().getRes().isEmpty()) {
                String message = "No person found for account id = " + account;
                log.error(message);
                throw new ApiException(message, ErrorCode.UNEXPECTED_ERROR, true);
            }
            return personRabbitResponse;
        } finally {
            if (personReplyQueueName != null) {
                rabbitAdmin.deleteQueue(personReplyQueueName);
            }
        }
    }

    public BaseMeter searchMetersByPersonId(String personId) {

        String metersReplyQueueName = null;

        try {
            metersReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties metersMessageProperties = createMessageProperties(metersReplyQueueName, SEARCH_METER_TYPE);
            BaseMeter bodyObject = createMetersRabbitRequest(personId);
            byte[] metersMessageBody = toJsonToBytes(bodyObject);
            Message metersRequestMessage = new Message(metersMessageBody, metersMessageProperties);

            template.send(ccbQueueName, metersRequestMessage);

            return safelyReceiveResponse(metersReplyQueueName, BaseMeter.class);
        } finally {
            if (metersReplyQueueName != null) {
                rabbitAdmin.deleteQueue(metersReplyQueueName);
            }
        }
    }

    public BaseMeter getMeterById(String meterId) {

        String meterValuesReplyQueueName = null;

        try {
            meterValuesReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties metersMessageProperties = createMessageProperties(meterValuesReplyQueueName, GET_METER_TYPE);
            BaseMeter bodyObject = createMeterValuesRabbitRequest(meterId);
            byte[] metersMessageBody = toJsonToBytes(bodyObject);
            Message meterValuesRequestMessage = new Message(metersMessageBody, metersMessageProperties);

            template.send(ccbQueueName, meterValuesRequestMessage);

            return safelyReceiveResponse(meterValuesReplyQueueName, BaseMeter.class);
        } finally {
            if (meterValuesReplyQueueName != null) {
                rabbitAdmin.deleteQueue(meterValuesReplyQueueName);
            }
        }
    }

    private BaseMeter createMeterValuesRabbitRequest(String meterId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);
        rabbitRequest.setId(meterId);
        return rabbitRequest;
    }

    private BaseMeter createMetersRabbitRequest(String personId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);

        BaseMeter.Srch search = new BaseMeter.Srch();
        search.setPerson_Id(personId);
        rabbitRequest.setSrch(search);
        return rabbitRequest;
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
}
