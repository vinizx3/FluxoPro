package com.vftech.fluxopro.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record FieldChange(
        @Column(name = "field", nullable = false, length = 100) String field,
        @Column(name = "old_value", columnDefinition = "text") String oldValue,
        @Column(name = "new_value", columnDefinition = "text") String newValue) {
}
