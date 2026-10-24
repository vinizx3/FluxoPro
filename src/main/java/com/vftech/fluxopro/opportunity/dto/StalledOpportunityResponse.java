package com.vftech.fluxopro.opportunity.dto;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record StalledOpportunityResponse(
        UUID id,
        String title,
        UUID customerId,
        String customerName,
        BigDecimal amount,
        OpportunityStatus status,
        Instant statusChangedAt,
        long daysStalled) {

    public static StalledOpportunityResponse from(Opportunity opportunity, Instant now) {
        return new StalledOpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getCustomer().getId(),
                opportunity.getCustomer().getName(),
                opportunity.getAmount(),
                opportunity.getStatus(),
                opportunity.getStatusChangedAt(),
                Duration.between(opportunity.getStatusChangedAt(), now).toDays());
    }
}
