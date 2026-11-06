package com.vftech.fluxopro.contact.dto;

import com.vftech.fluxopro.contact.entity.ContactKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterContactRequest(
        @NotNull ContactKind kind,
        @NotBlank @Size(max = 1000) String note,
        LocalDate nextStepAt,
        @Size(max = 255) String nextStepNote) {
}
