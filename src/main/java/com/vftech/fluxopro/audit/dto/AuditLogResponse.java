package com.vftech.fluxopro.audit.dto;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        AuditEntityType entityType,
        UUID entityId,
        AuditAction action,
        Instant occurredAt,
        List<AuditChangeResponse> changes) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor().getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getOccurredAt(),
                log.getChanges().stream()
                        .map(c -> new AuditChangeResponse(c.field(), c.oldValue(), c.newValue()))
                        .toList());
    }
}
