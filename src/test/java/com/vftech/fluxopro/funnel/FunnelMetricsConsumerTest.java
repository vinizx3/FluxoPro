package com.vftech.fluxopro.funnel;

import com.vftech.fluxopro.funnel.messaging.FunnelMetricsConsumer;
import com.vftech.fluxopro.funnel.service.FunnelProjection;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.support.EventRecords;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FunnelMetricsConsumerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final FunnelProjection projection = mock(FunnelProjection.class);
    private final FunnelMetricsConsumer consumer = new FunnelMetricsConsumer(projection, json);

    @Test
    void appliesTheCreationOfAnOpportunity() {
        UUID eventId = UUID.randomUUID();

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_CREATED",
                EventRecords.payload(null, "LEAD")));

        ArgumentCaptor<OpportunityEventPayload> received = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(projection).apply(eq(eventId), received.capture());
        assertThat(received.getValue().toStatus()).isEqualTo("LEAD");
    }

    @Test
    void appliesAStatusChange() {
        UUID eventId = UUID.randomUUID();

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_STATUS_CHANGED",
                EventRecords.payload("LEAD", "QUALIFICATION")));

        ArgumentCaptor<OpportunityEventPayload> received = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(projection).apply(eq(eventId), received.capture());
        assertThat(received.getValue().fromStatus()).isEqualTo("LEAD");
        assertThat(received.getValue().toStatus()).isEqualTo("QUALIFICATION");
    }

    @Test
    void ignoresEventTypesItDoesNotKnow() {
        consumer.onEvent(EventRecords.record(json, UUID.randomUUID(), "SOMETHING_ELSE",
                EventRecords.payload("LEAD", "QUALIFICATION")));

        verifyNoInteractions(projection);
    }

    @Test
    void rejectsAMalformedPayloadSoItGoesToTheDeadLetterTopic() {
        assertThatThrownBy(() -> consumer.onEvent(EventRecords.rawRecord(
                UUID.randomUUID(), "OPPORTUNITY_CREATED", "key", "not json")))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(projection);
    }
}
