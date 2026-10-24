package com.vftech.fluxopro.notification.controller;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.CurrentUser;
import com.vftech.fluxopro.notification.dto.MarkedReadResponse;
import com.vftech.fluxopro.notification.dto.NotificationResponse;
import com.vftech.fluxopro.notification.dto.UnreadCountResponse;
import com.vftech.fluxopro.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(currentUser.get(), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(service.unreadCount(currentUser.get()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return service.markRead(currentUser.get(), id);
    }

    @PostMapping("/read-all")
    public MarkedReadResponse markAllRead() {
        return new MarkedReadResponse(service.markAllRead(currentUser.get()));
    }
}
