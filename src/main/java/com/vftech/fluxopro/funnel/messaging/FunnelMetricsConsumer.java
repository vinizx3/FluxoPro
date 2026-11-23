package com.vftech.fluxopro.funnel.messaging;

import com.vftech.fluxopro.common.messaging.EventHeaders;
import com.vftech.fluxopro.common.messaging.KafkaTopicsConfig;
import com.vftech.fluxopro.funnel.service.FunnelProjection;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.UUID;

@Component
public class FunnelMetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(FunnelMetricsConsumer.class);
    private static final Set<String> HANDLED = Set.of("OPPORTUNITY_CREATED", "OPPORTUNITY_STATUS_CHANGED");

    private final FunnelProjection projection;
    private final JsonMapper json;

    public FunnelMetricsConsumer(FunnelProjection projection, JsonMapper json) {
        this.projection = projection;
        this.json = json;
    }

    @KafkaListener(topics = KafkaTopicsConfig.OPPORTUNITY_EVENTS, groupId = "funnel-metrics")
    public void onEvent(ConsumerRecord<String, String> record) {
        if (!HANDLED.contains(EventHeaders.get(record, "eventType"))) {
            return;
        }
        UUID eventId = UUID.fromString(EventHeaders.get(record, "eventId"));
        OpportunityEventPayload payload = json.readValue(record.value(), OpportunityEventPayload.class);
        boolean applied = projection.apply(eventId, payload);
        log.info("Event {} -> {}", eventId, applied ? "applied to the funnel" : "ignored (duplicate)");
    }
}
