package com.vftech.fluxopro.audit.controller;

import com.vftech.fluxopro.audit.dto.AuditLogResponse;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.service.AuditService;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/audit-log")
public class AuditController {

    private final AuditService service;
    private final CurrentUser currentUser;

    public AuditController(AuditService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) AuditEntityType entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(currentUser.get(), entityType, entityId, page, size);
    }
}
