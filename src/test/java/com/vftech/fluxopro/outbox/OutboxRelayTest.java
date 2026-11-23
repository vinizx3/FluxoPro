package com.vftech.fluxopro.outbox;

import com.vftech.fluxopro.common.messaging.KafkaTopicsConfig;
import com.vftech.fluxopro.outbox.entity.OutboxEvent;
import com.vftech.fluxopro.outbox.repository.OutboxEventRepository;
import com.vftech.fluxopro.outbox.service.OutboxRelay;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafka;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafka, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static OutboxEvent event() {
        OutboxEvent event = new OutboxEvent("OPPORTUNITY", UUID.randomUUID(),
                "OPPORTUNITY_STATUS_CHANGED", UUID.randomUUID(), "{}", NOW.minusSeconds(60));
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private static CompletableFuture<SendResult<String, String>> ack() {
        return CompletableFuture.completedFuture(null);
    }

    private static CompletableFuture<SendResult<String, String>> brokerDown() {
        return CompletableFuture.failedFuture(new IllegalStateException("broker down"));
    }

    @Test
    void publishesPendingEventsAndMarksThemAsPublished() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockPending(anyInt())).thenReturn(List.of(first, second));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(ack());

        relay.publishPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isEqualTo(NOW);
        verify(kafka, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    void leavesEventPendingWhenTheBrokerIsDown() {
        OutboxEvent first = event();
        when(repository.lockPending(anyInt())).thenReturn(List.of(first));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(brokerDown());

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNull();
    }

    @Test
    void stopsAtTheFirstFailureToKeepOrdering() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockPending(anyInt())).thenReturn(List.of(first, second));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(brokerDown());

        relay.publishPending();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(kafka, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void keepsWhatWasAlreadyPublishedWhenALaterEventFails() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockPending(anyInt())).thenReturn(List.of(first, second));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(ack(), brokerDown());

        relay.publishPending();

        assertThat(first.getPublishedAt()).isEqualTo(NOW);
        assertThat(second.getPublishedAt()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesTheAggregateIdAsKeyAndSendsTheEventHeaders() {
        OutboxEvent event = event();
        when(repository.lockPending(anyInt())).thenReturn(List.of(event));
        when(kafka.send(any(ProducerRecord.class))).thenReturn(ack());

        relay.publishPending();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(captor.capture());
        ProducerRecord<String, String> sent = captor.getValue();
        assertThat(sent.topic()).isEqualTo(KafkaTopicsConfig.OPPORTUNITY_EVENTS);
        assertThat(sent.key()).isEqualTo(event.getAggregateId().toString());
        assertThat(new String(sent.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8))
                .isEqualTo(event.getId().toString());
        assertThat(new String(sent.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8))
                .isEqualTo("OPPORTUNITY_STATUS_CHANGED");
    }
}
