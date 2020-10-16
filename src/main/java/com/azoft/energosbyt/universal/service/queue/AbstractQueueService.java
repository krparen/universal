package com.azoft.energosbyt.universal.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

@Slf4j
public class AbstractQueueService {

    protected MessageProperties createMessageProperties(String type) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("type", type);
        messageProperties.setHeader("m_guid", "08.06.2020"); // легаси заголовок, должен присутствовать, а что в нём - не важно
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());
        return messageProperties;
    }
}
