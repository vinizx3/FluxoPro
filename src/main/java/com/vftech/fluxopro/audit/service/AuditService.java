package com.vftech.fluxopro.audit.service;

import com.vftech.fluxopro.audit.dto.AuditLogResponse;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.AuditLog;
import com.vftech.fluxopro.audit.repository.AuditLogRepository;
import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(AuthenticatedUser user, AuditEntityType entityType, UUID entityId, int page, int size) {
        if (!user.isOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners can view the audit log");
        }
        if (entityId != null && entityType == null) {
            throw new BusinessRuleException("entityType is required when entityId is given");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("occurredAt").descending().and(Sort.by("id").ascending()));

        Page<AuditLog> result;
        if (entityType == null) {
            result = logs.findByCompanyId(user.companyId(), pageable);
        } else if (entityId == null) {
            result = logs.findByCompanyIdAndEntityType(user.companyId(), entityType, pageable);
        } else {
            result = logs.findByCompanyIdAndEntityTypeAndEntityId(user.companyId(), entityType, entityId, pageable);
        }
        return PageResponse.of(result, AuditLogResponse::from);
    }
}
