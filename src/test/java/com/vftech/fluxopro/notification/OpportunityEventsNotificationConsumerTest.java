package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.messaging.OpportunityEventsNotificationConsumer;
import com.vftech.fluxopro.notification.service.ClosedOpportunityNotifier;
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

class OpportunityEventsNotificationConsumerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ClosedOpportunityNotifier notifier = mock(ClosedOpportunityNotifier.class);
    private final OpportunityEventsNotificationConsumer consumer =
            new OpportunityEventsNotificationConsumer(notifier, json);

    @Test
    void createsAWonNotificationWhenAnOpportunityIsWon() {
        UUID eventId = UUID.randomUUID();
        OpportunityEventPayload payload = EventRecords.payload("NEGOTIATION", "WON");

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_STATUS_CHANGED", payload));

        ArgumentCaptor<OpportunityEventPayload> received = ArgumentCaptor.forClass(OpportunityEventPayload.class);
        verify(notifier).notifyClosed(eq(eventId), eq(NotificationType.OPPORTUNITY_WON), received.capture());
        assertThat(received.getValue().opportunityId()).isEqualTo(payload.opportunityId());
        assertThat(received.getValue().amount()).isEqualByComparingTo(payload.amount());
    }

    @Test
    void createsALostNotificationWhenAnOpportunityIsLost() {
        UUID eventId = UUID.randomUUID();

        consumer.onEvent(EventRecords.record(json, eventId, "OPPORTUNITY_STATUS_CHANGED",
                EventRecords.payload("PROPOSAL", "LOST")));

        verify(notifier).notifyClosed(eq(eventId), eq(NotificationType.OPPORTUNITY_LOST),
                org.mockito.ArgumentMatchers.any(OpportunityEventPayload.class));
    }

    @Test
    void ignoresEventsThatAreNotStatusChanges() {
        consumer.onEvent(EventRecords.record(json, UUID.randomUUID(), "OPPORTUNITY_CREATED",
                EventRecords.payload(null, "LEAD")));

        verifyNoInteractions(notifier);
    }

    @Test
    void ignoresStatusChangesThatDoNotCloseTheOpportunity() {
        consumer.onEvent(EventRecords.record(json, UUID.randomUUID(), "OPPORTUNITY_STATUS_CHANGED",
                EventRecords.payload("QUALIFICATION", "PROPOSAL")));

        verifyNoInteractions(notifier);
    }

    @Test
    void rejectsAMalformedPayloadSoItGoesToTheDeadLetterTopic() {
        assertThatThrownBy(() -> consumer.onEvent(EventRecords.rawRecord(
                UUID.randomUUID(), "OPPORTUNITY_STATUS_CHANGED", "key", "not json")))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notifier);
    }
}
