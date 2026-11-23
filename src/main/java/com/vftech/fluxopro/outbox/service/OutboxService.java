package com.vftech.fluxopro.outbox.service;

import com.vftech.fluxopro.outbox.entity.OutboxEvent;
import com.vftech.fluxopro.outbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper json;
    private final Clock clock;

    public OutboxService(OutboxEventRepository outboxEventRepository, JsonMapper json, Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.json = json;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String aggregateType, UUID aggregateId, String eventType, UUID companyId, Object payload) {
        outboxEventRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, companyId, json.writeValueAsString(payload), Instant.now(clock)));
    }
}