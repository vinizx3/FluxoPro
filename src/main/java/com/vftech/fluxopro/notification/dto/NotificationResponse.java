package com.vftech.fluxopro.notification.dto;

import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import com.vftech.fluxopro.opportunity.entity.Opportunity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID opportunityId,
        String opportunityTitle,
        String customerName,
        Instant stalledSince,
        long daysStalled,
        Instant createdAt,
        Instant readAt) {

    public static NotificationResponse from(Notification notification, Instant now) {
        Opportunity opportunity = notification.getOpportunity();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getCustomer().getName(),
                notification.getStalledSince(),
                Duration.between(notification.getStalledSince(), now).toDays(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}

