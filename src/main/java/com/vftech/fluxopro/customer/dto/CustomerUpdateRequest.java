package com.vftech.fluxopro.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotNull Long version) {
}