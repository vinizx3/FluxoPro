package com.vftech.fluxopro.opportunity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;


class OpportunityStatusTest {

    @ParameterizedTest
    @CsvSource({
            "LEAD,QUALIFICATION", "LEAD,LOST",
            "QUALIFICATION,LEAD", "QUALIFICATION,PROPOSAL", "QUALIFICATION,LOST",
            "PROPOSAL,QUALIFICATION", "PROPOSAL,NEGOTIATION", "PROPOSAL,LOST",
            "NEGOTIATION,PROPOSAL", "NEGOTIATION,WON", "NEGOTIATION,LOST"
    })
    void allowsValidTransitions(OpportunityStatus from, OpportunityStatus to) {
        assertThat(from.canMoveTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "LEAD,PROPOSAL", "LEAD,NEGOTIATION", "LEAD,WON",
            "QUALIFICATION,NEGOTIATION", "QUALIFICATION,WON",
            "PROPOSAL,LEAD", "PROPOSAL,WON",
            "NEGOTIATION,LEAD", "NEGOTIATION,QUALIFICATION"
    })
    void rejectsSkippingStages(OpportunityStatus from, OpportunityStatus to) {
        assertThat(from.canMoveTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OpportunityStatus.class, names = {"WON", "LOST"})
    void finalStatusesCannotMove(OpportunityStatus from) {
        for (OpportunityStatus to : OpportunityStatus.values()) {
            assertThat(from.canMoveTo(to)).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(OpportunityStatus.class)
    void cannotMoveToSameStatus(OpportunityStatus status) {
        assertThat(status.canMoveTo(status)).isFalse();
    }
}
