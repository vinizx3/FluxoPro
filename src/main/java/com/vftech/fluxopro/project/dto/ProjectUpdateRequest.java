package com.vftech.fluxopro.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        LocalDate expectedDeliveryDate,
        @NotNull Long version) {
}
