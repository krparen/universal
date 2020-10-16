package com.azoft.energosbyt.universal.service;

import com.azoft.energosbyt.universal.exception.ApiException;
import com.azoft.energosbyt.universal.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
public class RabbitService {

    private static final String HEADER_REPLY_TO = "reply-to";

    @Value("${energosbyt.rabbit.request.timeout-in-ms}")
    protected Long requestTimeout;

    @Autowired
    protected AmqpTemplate template;
    @Autowired
    protected AmqpAdmin rabbitAdmin;
    @Autowired
    protected ObjectMapper mapper;


    public <T> T sendAndReceive(String queueName, MessageProperties messageProperties, T messageBody) {
        String replyQueueName = messageProperties.getHeader(HEADER_REPLY_TO);

        try {
            if (replyQueueName == null || replyQueueName.isEmpty()) {
                replyQueueName = declareReplyQueueWithUuidName();
                messageProperties.setHeader(HEADER_REPLY_TO, replyQueueName);
            }

            Message personRequestMessage = new Message(toJsonToBytes(messageBody), messageProperties);

            template.send(queueName, personRequestMessage);
            T response = (T) safelyReceiveResponse(replyQueueName, messageBody.getClass());
            return response;
        } finally {

        }
    }

    protected String declareReplyQueueWithUuidName() {
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

    protected byte[] toJsonToBytes(Object bodyObject) {

        String bodyAsString = null;
        try {
            bodyAsString = mapper.writeValueAsString(bodyObject);
        } catch (JsonProcessingException e) {
            String message = "Rabbit request serialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }
        log.info("body as String: {}", bodyAsString);

        return bodyAsString.getBytes(StandardCharsets.UTF_8);
    }

    protected <T> T safelyReceiveResponse(String replyQueueName, Class<T> responseClass) {
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

        String responseAsString = getMessageBodyAsString(responseMessage);

        T response = null;
        try {
            response = mapper.readValue(responseAsString, responseClass);
        } catch (JsonProcessingException e) {
            String message = "Rabbit response deserialization failed";
            log.error(message, e);
            throw new ApiException(message, e, ErrorCode.UNEXPECTED_ERROR);
        }

        log.info("response from rabbit: {}", response);
        return response;
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
}
