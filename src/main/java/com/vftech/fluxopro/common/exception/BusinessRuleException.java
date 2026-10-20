package com.vftech.fluxopro.common.exception;

import jakarta.persistence.Version;

import java.time.Instant;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    @Version
    private Long version;

    private Instant archivedAt;

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Instant getArchivedAt() { return archivedAt; }
}
