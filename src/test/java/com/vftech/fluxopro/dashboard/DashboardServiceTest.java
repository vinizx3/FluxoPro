package com.vftech.fluxopro.dashboard;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.dashboard.dto.DashboardResponse;
import com.vftech.fluxopro.dashboard.service.DashboardService;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Mock
    OpportunityRepository opportunities;
    @Mock
    CompanyRepository companies;

    private DashboardService service() {
        return new DashboardService(opportunities, companies, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aSellerCannotSeeTheDashboard() {
        AuthenticatedUser seller = new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), Role.SELLER);

        assertThatThrownBy(() -> service().build(seller, 90))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(opportunities, companies);
    }

    @Test
    void thePeriodIsClampedAndOnlyClosedDealsInsideItAreRequested() {
        UUID companyId = UUID.randomUUID();
        AuthenticatedUser owner = new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
        when(companies.findById(companyId)).thenReturn(Optional.of(new Company("Acme")));
        when(opportunities.findByCompanyIdAndStatusIn(eq(companyId), anyCollection())).thenReturn(List.of());
        when(opportunities.findByCompanyIdAndStatusInAndStatusChangedAtAfter(
                eq(companyId), anyCollection(), eq(NOW.minus(365, ChronoUnit.DAYS)))).thenReturn(List.of());

        DashboardResponse response = service().build(owner, 9999);

        assertThat(response.periodDays()).isEqualTo(365);
    }

    @Test
    void theCompanysOwnThresholdDecidesWhatIsStalled() {
        UUID companyId = UUID.randomUUID();
        AuthenticatedUser owner = new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
        Company company = new Company("Acme");
        company.changeStalledAfterDays(3);
        User seller = mock(User.class);
        lenient().when(seller.getId()).thenReturn(UUID.randomUUID());
        lenient().when(seller.getName()).thenReturn("Ana");
        Opportunity fiveDaysQuiet = new Opportunity(null, null, seller, "Deal", new BigDecimal("1000"),
                NOW.minus(5, ChronoUnit.DAYS));
        when(companies.findById(companyId)).thenReturn(Optional.of(company));
        when(opportunities.findByCompanyIdAndStatusIn(eq(companyId), anyCollection()))
                .thenReturn(List.of(fiveDaysQuiet));
        when(opportunities.findByCompanyIdAndStatusInAndStatusChangedAtAfter(
                eq(companyId), anyCollection(), any())).thenReturn(List.of());

        DashboardResponse response = service().build(owner, 90);

        assertThat(response.stalledCount()).isEqualTo(1);
    }
}
