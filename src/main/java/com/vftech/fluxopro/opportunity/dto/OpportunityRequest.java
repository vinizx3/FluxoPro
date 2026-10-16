package com.vftech.fluxopro.opportunity.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record OpportunityRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 200) String title,
        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal amount) {
}
