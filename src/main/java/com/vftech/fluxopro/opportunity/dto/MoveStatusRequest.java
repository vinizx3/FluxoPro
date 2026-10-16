package com.vftech.fluxopro.opportunity.dto;

import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MoveStatusRequest(
        @NotNull OpportunityStatus status,
        @Size(max = 255) String lostReason) {
}
