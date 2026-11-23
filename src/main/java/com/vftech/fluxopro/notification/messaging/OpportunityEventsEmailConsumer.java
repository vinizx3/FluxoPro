package com.vftech.fluxopro.notification.messaging;

import com.vftech.fluxopro.common.messaging.KafkaTopicsConfig;
import com.vftech.fluxopro.notification.service.ClosedOpportunityMailer;
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
public class OpportunityEventsEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(OpportunityEventsEmailConsumer.class);

    private final ClosedOpportunityMailer mailer;
    private final JsonMapper json;

    public OpportunityEventsEmailConsumer(ClosedOpportunityMailer mailer, JsonMapper json) {
        this.mailer = mailer;
        this.json = json;
    }

    @KafkaListener(topics = KafkaTopicsConfig.OPPORTUNITY_EVENTS, groupId = "emails")
    public void onEvent(ConsumerRecord<String, String> record) {
        if (!"OPPORTUNITY_STATUS_CHANGED".equals(header(record, "eventType"))) {
            return;
        }
        UUID eventId = UUID.fromString(header(record, "eventId"));
        OpportunityEventPayload payload = json.readValue(record.value(), OpportunityEventPayload.class);
        if (!"WON".equals(payload.toStatus()) && !"LOST".equals(payload.toStatus())) {
            return;
        }
        int sent = mailer.sendClosed(eventId, payload);
        log.info("Event {} ({}) -> {} e-mail(s) sent", eventId, payload.toStatus(), sent);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
