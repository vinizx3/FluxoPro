package com.vftech.fluxopro.opportunity.dto;

import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID ownerId,
        String ownerName,
        String title,
        BigDecimal amount,
        OpportunityStatus status,
        String lostReason,
        Instant statusChangedAt) {

    public static OpportunityResponse from(Opportunity o) {
        return new OpportunityResponse(
                o.getId(),
                o.getCustomer().getId(),
                o.getCustomer().getName(),
                o.getOwner().getId(),
                o.getOwner().getName(),
                o.getTitle(),
                o.getAmount(),
                o.getStatus(),
                o.getLostReason(),
                o.getStatusChangedAt());
    }
}
