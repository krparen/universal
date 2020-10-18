package com.azoft.energosbyt.universal.service.queue;

import com.azoft.energosbyt.universal.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PblQueueService {

    @Autowired
    private RabbitService rabbitService;
}
