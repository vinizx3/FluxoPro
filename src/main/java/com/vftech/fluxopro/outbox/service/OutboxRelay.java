package com.vftech.fluxopro.outbox.service;

import com.vftech.fluxopro.common.messaging.KafkaTopicsConfig;
import com.vftech.fluxopro.outbox.entity.OutboxEvent;
import com.vftech.fluxopro.outbox.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 50;
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka, Clock clock) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outbox.lockPending(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            if (!send(event)) {
                return; // keeps ordering: never skip an event and publish a later one
            }
            event.markPublished(Instant.now(clock));
        }
    }

    private boolean send(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaTopicsConfig.OPPORTUNITY_EVENTS,
                event.getAggregateId().toString(),
                event.getPayload());
        record.headers().add("eventId", event.getId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));
        try {
            kafka.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Could not publish outbox event {}: {}", event.getId(), e.toString());
            return false;
        }
    }
}