package com.vftech.fluxopro.opportunity.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OpportunityUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull Long version) {
}
