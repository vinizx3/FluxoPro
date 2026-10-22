package com.vftech.fluxopro.project.dto;

import com.vftech.fluxopro.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusRequest(
        @NotNull ProjectStatus status,
        @NotNull Long version) {
}
