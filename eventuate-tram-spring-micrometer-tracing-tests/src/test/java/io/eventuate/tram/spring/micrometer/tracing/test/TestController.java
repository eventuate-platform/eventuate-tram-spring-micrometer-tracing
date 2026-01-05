package io.eventuate.tram.spring.micrometer.tracing.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import io.eventuate.tram.messaging.producer.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    public static final String TEST_CHANNEL = "test-channel";

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(path = "/send/{id}")
    public String sendMessage(@RequestBody TestMessage testMessage, @PathVariable("id") String id) throws JsonProcessingException {
        Message message = MessageBuilder
                .withPayload(objectMapper.writeValueAsString(testMessage))
                .build();
        messageProducer.send(TEST_CHANNEL, message);
        return message.getId();
    }
}
