package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import com.vftech.fluxopro.opportunity.entity.Opportunity;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class StalledAlertService {

    private static final List<OpportunityStatus> OPEN_STATUSES = Arrays.stream(OpportunityStatus.values())
            .filter(OpportunityStatus::isOpen)
            .toList();

    private final OpportunityRepository opportunities;
    private final NotificationRepository notifications;
    private final Clock clock;

    public StalledAlertService(OpportunityRepository opportunities,
                               NotificationRepository notifications,
                               Clock clock) {
        this.opportunities = opportunities;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public int createAlerts(UUID companyId, int stalledAfterDays) {
        Instant now = Instant.now(clock);
        Instant limit = now.minus(Duration.ofDays(stalledAfterDays));

        int created = 0;
        for (Opportunity opportunity : opportunities
                .findByCompanyIdAndArchivedAtIsNullAndStatusInAndStatusChangedAtBefore(
                        companyId, OPEN_STATUSES, limit)) {
            boolean alreadyAlerted = notifications.existsByOpportunityIdAndTypeAndStalledSince(
                    opportunity.getId(), NotificationType.STALLED_OPPORTUNITY, opportunity.getStatusChangedAt());
            if (alreadyAlerted) {
                continue;
            }
            notifications.save(new Notification(
                    opportunity.getCompany(),
                    opportunity.getOwner(),
                    NotificationType.STALLED_OPPORTUNITY,
                    opportunity,
                    opportunity.getStatusChangedAt(),
                    now));
            created++;
        }
        return created;
    }
}
