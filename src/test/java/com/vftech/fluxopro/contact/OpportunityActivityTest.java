package com.vftech.fluxopro.contact;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityActivityTest {

    private static final Instant CREATED = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-06T10:00:00Z");

    private Opportunity newOpportunity() {
        return new Opportunity(null, null, null, "Reforma", new BigDecimal("1000"), CREATED);
    }

    @Test
    void activityStartsAtCreation() {
        assertThat(newOpportunity().getLastActivityAt()).isEqualTo(CREATED);
    }

    @Test
    void registeringAContactRestartsTheClockAndStoresTheNextStep() {
        Opportunity o = newOpportunity();

        o.registerContact(LocalDate.of(2026, 11, 10), "Send revised proposal", LATER);

        assertThat(o.getLastActivityAt()).isEqualTo(LATER);
        assertThat(o.getStatusChangedAt()).isEqualTo(CREATED);
        assertThat(o.getNextStepAt()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(o.getNextStepNote()).isEqualTo("Send revised proposal");
    }

    @Test
    void aContactWithoutNextStepClearsThePreviousOne() {
        Opportunity o = newOpportunity();
        o.registerContact(LocalDate.of(2026, 11, 10), "Call back", LATER);

        o.registerContact(null, null, LATER.plusSeconds(60));

        assertThat(o.getNextStepAt()).isNull();
        assertThat(o.getNextStepNote()).isNull();
    }

    @Test
    void changingStatusCountsAsActivity() {
        Opportunity o = newOpportunity();

        o.changeStatus(OpportunityStatus.QUALIFICATION, null, LATER);

        assertThat(o.getLastActivityAt()).isEqualTo(LATER);
    }

    @Test
    void closingTheOpportunityClearsTheNextStep() {
        Opportunity o = newOpportunity();
        o.registerContact(LocalDate.of(2026, 11, 10), "Call back", LATER);

        o.changeStatus(OpportunityStatus.LOST, "Price", LATER.plusSeconds(60));

        assertThat(o.getNextStepAt()).isNull();
        assertThat(o.getNextStepNote()).isNull();
    }

    @Test
    void closedOpportunitiesRejectContacts() {
        Opportunity o = newOpportunity();
        o.changeStatus(OpportunityStatus.LOST, "Price", LATER);

        assertThatThrownBy(() -> o.registerContact(null, null, LATER.plusSeconds(60)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
