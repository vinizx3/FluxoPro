package com.vftech.fluxopro.opportunity.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReassignRequest(
        @NotNull UUID ownerId,
        @NotNull Long version) {
}
