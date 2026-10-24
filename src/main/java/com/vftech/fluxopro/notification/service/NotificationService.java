package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.notification.dto.NotificationResponse;
import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(AuthenticatedUser user, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<Notification> result = unreadOnly
                ? notifications.findByCompanyIdAndRecipientIdAndReadAtIsNull(user.companyId(), user.id(), pageable)
                : notifications.findByCompanyIdAndRecipientId(user.companyId(), user.id(), pageable);

        Instant now = Instant.now(clock);
        return PageResponse.of(result, n -> NotificationResponse.from(n, now));
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthenticatedUser user) {
        return notifications.countByCompanyIdAndRecipientIdAndReadAtIsNull(user.companyId(), user.id());
    }

    @Transactional
    public NotificationResponse markRead(AuthenticatedUser user, UUID id) {
        Notification notification = notifications
                .findByIdAndCompanyIdAndRecipientId(id, user.companyId(), user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        Instant now = Instant.now(clock);
        notification.markAsRead(now);
        return NotificationResponse.from(notification, now);
    }

    @Transactional
    public int markAllRead(AuthenticatedUser user) {
        return notifications.markAllRead(user.companyId(), user.id(), Instant.now(clock));
    }
}
