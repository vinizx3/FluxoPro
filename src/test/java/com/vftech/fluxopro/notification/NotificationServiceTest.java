package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.notification.dto.NotificationResponse;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import com.vftech.fluxopro.notification.service.NotificationService;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @Mock
    NotificationRepository notifications;

    private final UUID companyId = UUID.randomUUID();
    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notifications, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    private Notification notificationStalledFor(int days) {
        Instant since = NOW.minus(Duration.ofDays(days));
        Company company = new Company("Acme");
        Opportunity opportunity = new Opportunity(company, new Customer(company, "Client", null, null),
                mock(User.class), "Deal", null, since);
        return new Notification(company, mock(User.class), NotificationType.STALLED_OPPORTUNITY,
                opportunity, since, since);
    }

    @Test
    void listsOnlyTheCallersNotificationsWithDaysStalled() {
        AuthenticatedUser user = user();
        when(notifications.findByCompanyIdAndRecipientId(eq(companyId), eq(user.id()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificationStalledFor(10))));

        PageResponse<NotificationResponse> response = service.list(user, false, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).daysStalled()).isEqualTo(10);
        assertThat(response.content().get(0).opportunityTitle()).isEqualTo("Deal");
    }

    @Test
    void unreadOnlyUsesTheUnreadQuery() {
        AuthenticatedUser user = user();
        when(notifications.findByCompanyIdAndRecipientIdAndReadAtIsNull(
                eq(companyId), eq(user.id()), any(Pageable.class))).thenReturn(Page.empty());

        service.list(user, true, 0, 20);

        verify(notifications).findByCompanyIdAndRecipientIdAndReadAtIsNull(
                eq(companyId), eq(user.id()), any(Pageable.class));
    }

    @Test
    void unreadCountComesFromTheRepository() {
        AuthenticatedUser user = user();
        when(notifications.countByCompanyIdAndRecipientIdAndReadAtIsNull(companyId, user.id())).thenReturn(3L);

        assertThat(service.unreadCount(user)).isEqualTo(3L);
    }

    @Test
    void markReadStampsTheClock() {
        AuthenticatedUser user = user();
        UUID id = UUID.randomUUID();
        when(notifications.findByIdAndCompanyIdAndRecipientId(id, companyId, user.id()))
                .thenReturn(Optional.of(notificationStalledFor(10)));

        NotificationResponse response = service.markRead(user, id);

        assertThat(response.readAt()).isEqualTo(NOW);
    }

    @Test
    void notificationOfSomeoneElseIsNotFound() {
        AuthenticatedUser user = user();
        UUID id = UUID.randomUUID();
        when(notifications.findByIdAndCompanyIdAndRecipientId(id, companyId, user.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(user, id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void markAllReadReturnsHowManyWereUpdated() {
        AuthenticatedUser user = user();
        when(notifications.markAllRead(companyId, user.id(), NOW)).thenReturn(4);

        assertThat(service.markAllRead(user)).isEqualTo(4);
    }
}
