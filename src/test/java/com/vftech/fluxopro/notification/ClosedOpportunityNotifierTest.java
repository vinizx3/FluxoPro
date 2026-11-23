package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import com.vftech.fluxopro.notification.service.ClosedOpportunityNotifier;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClosedOpportunityNotifierTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private NotificationRepository notifications;
    @Mock
    private UserRepository users;
    @Mock
    private CompanyRepository companies;
    @Mock
    private OpportunityRepository opportunities;

    private final UUID companyId = UUID.randomUUID();
    private final UUID companyOwnerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID opportunityId = UUID.randomUUID();
    private final Company company = new Company("Acme");

    private ClosedOpportunityNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new ClosedOpportunityNotifier(notifications, users, companies, opportunities, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User user(UUID id, Role role) {
        User user = new User(company, "User", id + "@acme.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Opportunity opportunityOwnedBy(User owner) {
        return new Opportunity(company, new Customer(company, "Client", null, null), owner, "Deal", BigDecimal.TEN, NOW);
    }

    private OpportunityEventPayload payload(UUID ownerId) {
        return new OpportunityEventPayload(opportunityId, companyId, UUID.randomUUID(), ownerId, ownerId, "Deal", BigDecimal.TEN, "NEGOTIATION", "WON", NOW);
    }

    @Test
    void notifiesTheCompanyOwnersAndTheOpportunityOwner() {
        User companyOwner = user(companyOwnerId, Role.OWNER);
        User seller = user(sellerId, Role.SELLER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(companies.getReferenceById(companyId)).thenReturn(company);
        when(users.getReferenceById(companyOwnerId)).thenReturn(companyOwner);
        when(users.getReferenceById(sellerId)).thenReturn(seller);
        when(opportunities.getReferenceById(opportunityId)).thenReturn(opportunityOwnedBy(seller));

        int created = notifier.notifyClosed(eventId, NotificationType.OPPORTUNITY_WON, payload(sellerId));

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.getType()).isEqualTo(NotificationType.OPPORTUNITY_WON);
            assertThat(notification.getSourceEventId()).isEqualTo(eventId);
            assertThat(notification.getStalledSince()).isNull();
        });
        assertThat(saved.getAllValues().stream().map(n -> n.getRecipient().getId()).toList())
                .containsExactlyInAnyOrder(companyOwnerId, sellerId);
    }

    @Test
    void doesNotNotifyTwiceWhenTheOpportunityOwnerIsAlsoACompanyOwner() {
        User ownerAndSeller = user(sellerId, Role.OWNER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(ownerAndSeller));
        when(companies.getReferenceById(companyId)).thenReturn(company);
        when(users.getReferenceById(sellerId)).thenReturn(ownerAndSeller);
        when(opportunities.getReferenceById(opportunityId)).thenReturn(opportunityOwnedBy(ownerAndSeller));

        int created = notifier.notifyClosed(eventId, NotificationType.OPPORTUNITY_WON, payload(sellerId));

        assertThat(created).isEqualTo(1);
        verify(notifications, times(1)).save(any(Notification.class));
    }

    @Test
    void skipsRecipientsThatAlreadyReceivedTheEvent() {
        User companyOwner = user(companyOwnerId, Role.OWNER);
        when(users.findByCompanyIdAndRoleAndActiveTrue(companyId, Role.OWNER)).thenReturn(List.of(companyOwner));
        when(notifications.existsBySourceEventIdAndRecipientId(eventId, companyOwnerId)).thenReturn(true);

        int created = notifier.notifyClosed(eventId, NotificationType.OPPORTUNITY_WON, payload(companyOwnerId));

        assertThat(created).isZero();
        verify(notifications, never()).save(any(Notification.class));
    }
}
