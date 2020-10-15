package com.azoft.energosbyt.universal.service.queue;

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

    public  BasePerson searchPersonByAccount(String account) {

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
