package com.vftech.fluxopro.audit.service;

import com.vftech.fluxopro.audit.entity.FieldChange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ChangeSet {

    private final List<FieldChange> changes = new ArrayList<>();

    public static ChangeSet create() {
        return new ChangeSet();
    }

    public ChangeSet track(String field, Object before, Object after) {
        if (!sameValue(before, after)) {
            changes.add(new FieldChange(field, asText(before), asText(after)));
        }
        return this;
    }

    public List<FieldChange> build() {
        return List.copyOf(changes);
    }

    private static boolean sameValue(Object before, Object after) {
        if (before instanceof BigDecimal a && after instanceof BigDecimal b) {
            return a.compareTo(b) == 0;
        }
        return Objects.equals(before, after);
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
