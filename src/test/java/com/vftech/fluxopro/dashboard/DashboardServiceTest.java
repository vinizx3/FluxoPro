package com.vftech.fluxopro.dashboard;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.dashboard.dto.DashboardResponse;
import com.vftech.fluxopro.dashboard.service.DashboardService;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Mock
    OpportunityRepository opportunities;

    private DashboardService service() {
        return new DashboardService(opportunities, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aSellerCannotSeeTheDashboard() {
        AuthenticatedUser seller = new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), Role.SELLER);

        assertThatThrownBy(() -> service().build(seller, 90))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(opportunities);
    }

    @Test
    void thePeriodIsClampedAndOnlyClosedDealsInsideItAreRequested() {
        UUID companyId = UUID.randomUUID();
        AuthenticatedUser owner = new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
        when(opportunities.findByCompanyIdAndStatusIn(eq(companyId), anyCollection())).thenReturn(List.of());
        when(opportunities.findByCompanyIdAndStatusInAndStatusChangedAtAfter(
                eq(companyId), anyCollection(), eq(NOW.minus(365, ChronoUnit.DAYS)))).thenReturn(List.of());

        DashboardResponse response = service().build(owner, 9999);

        assertThat(response.periodDays()).isEqualTo(365);
    }
}
