package com.vftech.fluxopro.opportunity.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OpportunityEventPayload(
        UUID opportunityId,
        UUID companyId,
        UUID customerId,
        UUID ownerId,
        UUID actorId,
        String title,
        BigDecimal amount,
        String fromStatus,
        String toStatus,
        Instant occurredAt) {
}