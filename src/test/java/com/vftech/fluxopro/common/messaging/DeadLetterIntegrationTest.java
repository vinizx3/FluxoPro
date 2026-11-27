package com.vftech.fluxopro.common.messaging;

import com.vftech.fluxopro.support.IntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class DeadLetterIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    @Autowired
    KafkaTemplate<String, String> template;

    @Autowired
    KafkaContainer kafka;

    @Test
    void malformedEventGoesStraightToTheDeadLetterTopic() throws Exception {
        String marker = "not-json-" + UUID.randomUUID();
        ProducerRecord<String, String> record = new ProducerRecord<>(
                KafkaTopicsConfig.OPPORTUNITY_EVENTS, UUID.randomUUID().toString(), marker);
        record.headers().add("eventId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", "OPPORTUNITY_STATUS_CHANGED".getBytes(StandardCharsets.UTF_8));

        template.send(record).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> dead = awaitInDeadLetterTopic(marker);

        assertThat(dead.value()).isEqualTo(marker);
        assertThat(headerValue(dead, "kafka_dlt-original-topic")).isEqualTo(KafkaTopicsConfig.OPPORTUNITY_EVENTS);
        assertThat(headerValue(dead, "kafka_dlt-exception-message")).isNotBlank();
    }

    private ConsumerRecord<String, String> awaitInDeadLetterTopic(String marker) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-dlt-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(KafkaTopicsConfig.OPPORTUNITY_EVENTS_DLT));
            long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> candidate : consumer.poll(Duration.ofMillis(500))) {
                    if (marker.equals(candidate.value())) {
                        return candidate;
                    }
                }
            }
        }
        throw new AssertionError("Message was not found in the dead-letter topic within " + TIMEOUT);
    }

    private static String headerValue(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
