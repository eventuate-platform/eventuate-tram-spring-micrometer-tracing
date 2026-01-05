package io.eventuate.tram.spring.micrometer.tracing.test;

import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.tram.messaging.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
public class TestConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

    public static final String SUBSCRIBER_ID = "test-subscriber";

    private final List<Message> receivedMessages = new CopyOnWriteArrayList<>();

    @Autowired
    private MessageConsumer messageConsumer;

    @Bean
    public TestConsumer testConsumerBean() {
        return this;
    }

    public void subscribe() {
        logger.info("Subscribing to channel: {}", TestController.TEST_CHANNEL);
        messageConsumer.subscribe(
                SUBSCRIBER_ID,
                Collections.singleton(TestController.TEST_CHANNEL),
                this::handleMessage
        );
    }

    private void handleMessage(Message message) {
        logger.info("Received message: {} with headers: {}", message.getId(), message.getHeaders());
        receivedMessages.add(message);
    }

    public List<Message> getReceivedMessages() {
        return receivedMessages;
    }

    public void clearMessages() {
        receivedMessages.clear();
    }
}
