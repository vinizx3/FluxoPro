package com.vftech.fluxopro.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectCreateRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        LocalDate expectedDeliveryDate) {
}
