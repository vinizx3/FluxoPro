package com.vftech.fluxopro.opportunity;

import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.opportunity.dto.StalledOpportunityResponse;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.opportunity.service.OpportunityService;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceStalledTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @Mock
    OpportunityRepository opportunities;
    @Mock
    CustomerRepository customers;
    @Mock
    CompanyRepository companies;
    @Mock
    UserRepository users;
    @Mock
    ApplicationEventPublisher events;
    @Mock
    AuditRecorder audit;

    private final UUID companyId = UUID.randomUUID();
    private OpportunityService service;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunities, customers, companies, users,
                Clock.fixed(NOW, ZoneOffset.UTC), events, audit);
    }

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    private AuthenticatedUser seller() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.SELLER);
    }

    private Opportunity opportunityStalledSince(Instant since) {
        Company company = new Company("Acme");
        Customer customer = new Customer(company, "Client", null, null);
        return new Opportunity(company, customer, mock(User.class), "Deal", new BigDecimal("5000"), since);
    }

    @Test
    void ownerSeesStalledOpportunitiesOfTheWholeCompanyWithDaysStalled() {
        Instant limit = NOW.minus(Duration.ofDays(7));
        Opportunity old = opportunityStalledSince(NOW.minus(Duration.ofDays(10)));
        when(opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(limit), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(old)));

        PageResponse<StalledOpportunityResponse> response = service.stalled(owner(), 7, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).daysStalled()).isEqualTo(10);
        assertThat(response.content().get(0).title()).isEqualTo("Deal");
    }

    @Test
    void sellerOnlySeesTheirOwnStalledOpportunities() {
        AuthenticatedUser seller = seller();
        when(opportunities.findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), eq(seller.id()), anyCollection(), any(Instant.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.stalled(seller, 7, 0, 20);

        verify(opportunities).findByCompanyIdAndOwnerIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), eq(seller.id()), anyCollection(), any(Instant.class), any(Pageable.class));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "-5,1", "9999,365", "30,30"})
    void daysAreClampedBetweenOneAndAYear(int requested, int expectedDays) {
        Instant limit = NOW.minus(Duration.ofDays(expectedDays));
        when(opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(limit), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.stalled(owner(), requested, 0, 20);

        verify(opportunities).findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(limit), any(Pageable.class));
    }
}
