package com.vftech.fluxopro.audit;

import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.service.ChangeSet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeSetTest {

    @Test
    void tracksOnlyFieldsThatActuallyChanged() {
        var changes = ChangeSet.create()
                .track("title", "Old", "New")
                .track("owner", "same", "same")
                .build();

        assertThat(changes).containsExactly(new FieldChange("title", "Old", "New"));
    }

    @Test
    void nothingChangedGivesAnEmptyList() {
        assertThat(ChangeSet.create().track("title", "A", "A").build()).isEmpty();
    }

    @Test
    void nullsAreRecordedAsNullText() {
        var changes = ChangeSet.create().track("phone", null, "6199999").build();

        assertThat(changes).containsExactly(new FieldChange("phone", null, "6199999"));
    }

    @Test
    void valuesAreStoredAsText() {
        var changes = ChangeSet.create().track("amount", new BigDecimal("5000.00"), new BigDecimal("7000.00")).build();

        assertThat(changes).containsExactly(new FieldChange("amount", "5000.00", "7000.00"));
    }

    @Test
    void bigDecimalsWithDifferentScaleAreTheSameValue() {
        var changes = ChangeSet.create().track("amount", new BigDecimal("5000"), new BigDecimal("5000.00")).build();

        assertThat(changes).isEmpty();
    }
}
