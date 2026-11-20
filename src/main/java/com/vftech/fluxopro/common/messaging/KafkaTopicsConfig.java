package com.vftech.fluxopro.common.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    public static final String OPPORTUNITY_EVENTS = "opportunity.events";
    public static final String OPPORTUNITY_EVENTS_DLT = "opportunity.events.DLT";

    @Bean
    NewTopic opportunityEventsTopic() {
        return TopicBuilder.name(OPPORTUNITY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic opportunityEventsDeadLetterTopic() {
        return TopicBuilder.name(OPPORTUNITY_EVENTS_DLT).partitions(3).replicas(1).build();
    }
}
