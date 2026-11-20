package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import com.vftech.fluxopro.opportunity.event.OpportunityEventPayload;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class ClosedOpportunityNotifier {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final CompanyRepository companies;
    private final OpportunityRepository opportunities;
    private final Clock clock;

    public ClosedOpportunityNotifier(NotificationRepository notifications, UserRepository users, CompanyRepository companies, OpportunityRepository opportunities,
                                     Clock clock) {
        this.notifications = notifications;
        this.users = users;
        this.companies = companies;
        this.opportunities = opportunities;
        this.clock = clock;
    }

    @Transactional
    public int notifyClosed(UUID eventId, NotificationType type, OpportunityEventPayload payload) {
        Set<UUID> recipients = new LinkedHashSet<>();
        for (User owner : users.findByCompanyIdAndRoleAndActiveTrue(payload.companyId(), Role.OWNER)) {
            recipients.add(owner.getId());
        }
        recipients.add(payload.ownerId());

        Instant now = Instant.now(clock);
        int created = 0;
        for (UUID recipientId : recipients) {
            if (notifications.existsBySourceEventIdAndRecipientId(eventId, recipientId)) {
                continue;
            }
            notifications.save(Notification.forClosedOpportunity(
                    companies.getReferenceById(payload.companyId()),
                    users.getReferenceById(recipientId),
                    type,
                    opportunities.getReferenceById(payload.opportunityId()),
                    eventId,
                    now));
            created++;
        }
        return created;
    }
}