package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import com.vftech.fluxopro.notification.service.StalledAlertService;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StalledAlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final Instant TEN_DAYS_AGO = NOW.minus(Duration.ofDays(10));

    @Mock
    OpportunityRepository opportunities;
    @Mock
    NotificationRepository notifications;

    private final UUID companyId = UUID.randomUUID();
    private StalledAlertService service;

    @BeforeEach
    void setUp() {
        service = new StalledAlertService(opportunities, notifications, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Opportunity stalledOpportunity(User owner) {
        Company company = new Company("Acme");
        return new Opportunity(company, new Customer(company, "Client", null, null),
                owner, "Deal", new BigDecimal("5000"), TEN_DAYS_AGO);
    }

    private void givenStalled(Opportunity... stalled) {
        when(opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(NOW.minus(Duration.ofDays(7)))))
                .thenReturn(List.of(stalled));
    }

    @Test
    void createsOneAlertForTheOwnerOfEachStalledOpportunity() {
        User owner = mock(User.class);
        givenStalled(stalledOpportunity(owner));

        int created = service.createAlerts(companyId, 7);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getRecipient()).isSameAs(owner);
        assertThat(saved.getType()).isEqualTo(NotificationType.STALLED_OPPORTUNITY);
        assertThat(saved.getStalledSince()).isEqualTo(TEN_DAYS_AGO);
        assertThat(saved.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotAlertAgainForTheSameStall() {
        givenStalled(stalledOpportunity(mock(User.class)));
        when(notifications.existsByOpportunityIdAndTypeAndStalledSince(
                any(), eq(NotificationType.STALLED_OPPORTUNITY), eq(TEN_DAYS_AGO))).thenReturn(true);

        int created = service.createAlerts(companyId, 7);

        assertThat(created).isZero();
        verify(notifications, never()).save(any());
    }

    @Test
    void nothingStalledMeansNoAlerts() {
        givenStalled();

        assertThat(service.createAlerts(companyId, 7)).isZero();
        verify(notifications, never()).save(any());
    }

    @Test
    void usesTheThresholdItWasGiven() {
        when(opportunities.findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(NOW.minus(Duration.ofDays(30)))))
                .thenReturn(List.of());

        service.createAlerts(companyId, 30);

        verify(opportunities).findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                eq(companyId), anyCollection(), eq(NOW.minus(Duration.ofDays(30))));
    }
}
