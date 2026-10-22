package com.vftech.fluxopro.project.dto;

import com.vftech.fluxopro.project.entity.Project;
import com.vftech.fluxopro.project.entity.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID customerId,
        String customerName,
        UUID sourceOpportunityId,
        UUID ownerId,
        String name,
        String description,
        ProjectStatus status,
        Instant statusChangedAt,
        LocalDate expectedDeliveryDate,
        Instant createdAt,
        Instant archivedAt,
        Long version) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getCustomer().getId(),
                project.getCustomer().getName(),
                project.getSourceOpportunity() == null ? null : project.getSourceOpportunity().getId(),
                project.getOwner().getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStatusChangedAt(),
                project.getExpectedDeliveryDate(),
                project.getCreatedAt(),
                project.getArchivedAt(),
                project.getVersion());
    }
}
