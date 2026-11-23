package com.vftech.fluxopro.support;

import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public final class EventRecords {

    private EventRecords() {
    }

    public static OpportunityEventPayload payload(String fromStatus, String toStatus) {
        return new OpportunityEventPayload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Deal", new BigDecimal("10000.00"), fromStatus, toStatus, Instant.parse("2026-11-23T12:00:00Z"));
    }

    public static ConsumerRecord<String, String> record(JsonMapper json, UUID eventId, String eventType, OpportunityEventPayload payload) {
        return rawRecord(eventId, eventType, payload.opportunityId().toString(), json.writeValueAsString(payload));
    }

    public static ConsumerRecord<String, String> rawRecord(UUID eventId, String eventType, String key, String value) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("opportunity.events", 0, 0L, key, value);
        record.headers().add("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", eventType.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
