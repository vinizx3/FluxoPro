package com.vftech.fluxopro.opportunity;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OpportunityReassignTest {

    private static final Instant CREATED = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-06T10:00:00Z");

    private Opportunity newOpportunity(User owner) {
        return new Opportunity(null, null, owner, "Reforma", new BigDecimal("1000"), CREATED);
    }

    @Test
    void anOpenOpportunityChangesOwner() {
        User alpha = mock(User.class);
        User beta = mock(User.class);
        Opportunity o = newOpportunity(alpha);

        o.reassign(beta);

        assertThat(o.getOwner()).isSameAs(beta);
    }

    @Test
    void reassigningDoesNotRestartTheThermometer() {
        Opportunity o = newOpportunity(mock(User.class));

        o.reassign(mock(User.class));

        assertThat(o.getLastActivityAt()).isEqualTo(CREATED);
    }

    @Test
    void aClosedOpportunityCannotBeReassigned() {
        Opportunity o = newOpportunity(mock(User.class));
        o.changeStatus(OpportunityStatus.LOST, "Price", LATER);

        assertThatThrownBy(() -> o.reassign(mock(User.class)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
