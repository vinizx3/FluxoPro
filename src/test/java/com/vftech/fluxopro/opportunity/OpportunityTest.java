package com.vftech.fluxopro.opportunity;

import com.vftech.fluxopro.Exception.InvalidStatusTransitionException;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpportunityTest {

    private static final Instant T0 = Instant.parse("2026-10-15T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(3600);

    private Opportunity newOpportunity() {
        Company company = new Company("Acme");
        Customer customer = new Customer(company, "Client", "client@example.com", null);
        User owner = new User(company, "Seller", "seller@acme.com", "hash", Role.SELLER);
        return new Opportunity(company, customer, owner, "Website redesign", new BigDecimal("15000.00"), T0);
    }

    @Test
    void startsAsLeadWithStatusDateSet() {
        Opportunity opportunity = newOpportunity();

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LEAD);
        assertThat(opportunity.getStatusChangedAt()).isEqualTo(T0);
    }

    @Test
    void movingStageUpdatesStatusAndDate() {
        Opportunity opportunity = newOpportunity();

        opportunity.changeStatus(OpportunityStatus.QUALIFICATION, null, T1);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.QUALIFICATION);
        assertThat(opportunity.getStatusChangedAt()).isEqualTo(T1);
    }

    @Test
    void invalidTransitionThrowsAndKeepsState() {
        Opportunity opportunity = newOpportunity();

        assertThatThrownBy(() -> opportunity.changeStatus(OpportunityStatus.WON, null, T1))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LEAD);
        assertThat(opportunity.getStatusChangedAt()).isEqualTo(T0);
    }

    @Test
    void losingRequiresAReason() {
        Opportunity opportunity = newOpportunity();

        assertThatThrownBy(() -> opportunity.changeStatus(OpportunityStatus.LOST, "  ", T1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LEAD);
    }

    @Test
    void losingStoresTheReason() {
        Opportunity opportunity = newOpportunity();

        opportunity.changeStatus(OpportunityStatus.LOST, " Chose a cheaper competitor ", T1);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LOST);
        assertThat(opportunity.getLostReason()).isEqualTo("Chose a cheaper competitor");
    }

    @Test
    void wonOpportunityCannotBeMovedAgain() {
        Opportunity opportunity = newOpportunity();
        opportunity.changeStatus(OpportunityStatus.QUALIFICATION, null, T1);
        opportunity.changeStatus(OpportunityStatus.PROPOSAL, null, T1);
        opportunity.changeStatus(OpportunityStatus.NEGOTIATION, null, T1);
        opportunity.changeStatus(OpportunityStatus.WON, null, T1);

        assertThatThrownBy(() -> opportunity.changeStatus(OpportunityStatus.NEGOTIATION, null, T1))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
