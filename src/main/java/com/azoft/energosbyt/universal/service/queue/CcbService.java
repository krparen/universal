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

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class CcbService extends AbstractQueueService {

    @Value("${energosbyt.rabbit.request.check.queue-name}")
    private String ccbQueueName;

    public BasePerson searchPersonByAccount(String account) {

        String personReplyQueueName = null;

        try {
            personReplyQueueName = declareReplyQueueWithUuidName();
            MessageProperties personMessageProperties = createPersonMessageProperties(personReplyQueueName);
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
            MessageProperties metersMessageProperties = createMetersMessageProperties(metersReplyQueueName);
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
            MessageProperties metersMessageProperties = createMeterValuesMessageProperties(meterValuesReplyQueueName);
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

    private MessageProperties createMeterValuesMessageProperties(String meterValuesReplyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "getMeter");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", meterValuesReplyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }

    private BaseMeter createMeterValuesRabbitRequest(String meterId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);
        rabbitRequest.setId(meterId);
        return rabbitRequest;
    }

    private MessageProperties createMetersMessageProperties(String metersReplyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "searchMeter");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", metersReplyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }

    private BaseMeter createMetersRabbitRequest(String personId) {
        BaseMeter rabbitRequest = new BaseMeter();
        rabbitRequest.setSystem_id(thisSystemId);

        BaseMeter.Srch search = new BaseMeter.Srch();
        search.setPerson_Id(personId);
        rabbitRequest.setSrch(search);
        return rabbitRequest;
    }

    private MessageProperties createPersonMessageProperties(String replyQueueName) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", "searchPerson");
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setHeader("reply-to", replyQueueName);
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
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
