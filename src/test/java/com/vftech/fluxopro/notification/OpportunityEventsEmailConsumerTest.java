package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.notification.messaging.OpportunityEventsEmailConsumer;
import com.vftech.fluxopro.notification.service.ClosedOpportunityMailer;
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

class OpportunityEventsEmailConsumerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ClosedOpportunityMailer mailer = mock(ClosedOpportunityMailer.class);
    private final OpportunityEventsEmailConsumer consumer = new OpportunityEventsEmailConsumer(mailer, json);

    @Test
    void sendsTheEmailWhenAnOpportunityIsWon() {
        UUID eventId = UUID.randomUUID();
        OpportunityEventPayload payload = EventRecords.payload("NEGOTIATION", "WON");

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_STATUS_CHANGED", payload));

        ArgumentCaptor<OpportunityEventPayload> received = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(mailer).sendClosed(eq(eventId), received.capture());
        assertThat(received.getValue().toStatus()).isEqualTo("WON");
        assertThat(received.getValue().opportunityId()).isEqualTo(payload.opportunityId());
    }

    @Test
    void sendsTheEmailWhenAnOpportunityIsLost() {
        UUID eventId = UUID.randomUUID();

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_STATUS_CHANGED",
                EventRecords.payload("PROPOSAL", "LOST")));

        ArgumentCaptor<OpportunityEventPayload> received = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(mailer).sendClosed(eq(eventId), received.capture());
        assertThat(received.getValue().toStatus()).isEqualTo("LOST");
    }

    @Test
    void ignoresEventsThatAreNotStatusChanges() {
        consumer.onEvent(EventRecords.record(json, UUID.randomUUID(), "OPPORTUNITY_CREATED",
                EventRecords.payload(null, "LEAD")));

        verifyNoInteractions(mailer);
    }

    @Test
    void ignoresStatusChangesThatDoNotCloseTheOpportunity() {
        consumer.onEvent(EventRecords.record(json, UUID.randomUUID(), "OPPORTUNITY_STATUS_CHANGED",
                EventRecords.payload("QUALIFICATION", "PROPOSAL")));

        verifyNoInteractions(mailer);
    }

    @Test
    void rejectsAMalformedPayloadSoItGoesToTheDeadLetterTopic() {
        assertThatThrownBy(() -> consumer.onEvent(EventRecords.rawRecord(
                UUID.randomUUID(), "OPPORTUNITY_STATUS_CHANGED", "key", "not json")))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(mailer);
    }
}