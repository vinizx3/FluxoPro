package com.vftech.fluxopro.funnel.service;

import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class FunnelProjection {

    public static final String CONSUMER = "funnel-metrics";

    private final JdbcClient jdbc;
    private final Clock clock;

    public FunnelProjection(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public boolean apply(UUID eventId, OpportunityEventPayload payload) {
        int inserted = jdbc.sql("""
                        INSERT INTO processed_event (consumer, event_id, processed_at)
                        VALUES (:consumer, :eventId, :now)
                        ON CONFLICT DO NOTHING
                        """)
                .param("consumer", CONSUMER)
                .param("eventId", eventId)
                .param("now", OffsetDateTime.now(clock))
                .update();
        if (inserted == 0) {
            return false;
        }
        jdbc.sql("""
                        INSERT INTO funnel_stage_entry (company_id, stage, entries, entered_amount)
                        VALUES (:company, :stage, 1, :amount)
                        ON CONFLICT (company_id, stage) DO UPDATE
                        SET entries = funnel_stage_entry.entries + 1,
                            entered_amount = funnel_stage_entry.entered_amount + EXCLUDED.entered_amount
                        """)
                .param("company", payload.companyId())
                .param("stage", payload.toStatus())
                .param("amount", payload.amount() == null ? BigDecimal.ZERO : payload.amount())
                .update();
        return true;
    }
}