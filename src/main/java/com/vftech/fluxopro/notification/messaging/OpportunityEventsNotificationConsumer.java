package com.vftech.fluxopro.notification.messaging;

import com.vftech.fluxopro.common.messaging.KafkaTopicsConfig;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.service.ClosedOpportunityNotifier;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OpportunityEventsNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OpportunityEventsNotificationConsumer.class);

    private final ClosedOpportunityNotifier notifier;
    private final JsonMapper json;

    public OpportunityEventsNotificationConsumer(ClosedOpportunityNotifier notifier, JsonMapper json) {
        this.notifier = notifier;
        this.json = json;
    }

    @KafkaListener(topics = KafkaTopicsConfig.OPPORTUNITY_EVENTS, groupId = "notifications")
    public void onEvent(ConsumerRecord<String, String> record) {
        if (!"OPPORTUNITY_STATUS_CHANGED".equals(header(record, "eventType"))) {
            return;
        }
        UUID eventId = UUID.fromString(header(record, "eventId"));
        OpportunityEventPayload payload = json.readValue(record.value(), OpportunityEventPayload.class);

        NotificationType type = switch (payload.toStatus()) {
            case "WON" -> NotificationType.OPPORTUNITY_WON;
            case "LOST" -> NotificationType.OPPORTUNITY_LOST;
            default -> null;
        };
        if (type == null) {
            return;
        }
        int created = notifier.notifyClosed(eventId, type, payload);
        log.info("Event {} ({}) -> {} notification(s) created", eventId, type, created);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}