package com.vftech.fluxopro.audit.service;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.AuditLog;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.repository.AuditLogRepository;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AuditRecorder {

    private final AuditLogRepository logs;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final Clock clock;

    public AuditRecorder(AuditLogRepository logs, CompanyRepository companies,
                         UserRepository users, Clock clock) {
        this.logs = logs;
        this.companies = companies;
        this.users = users;
        this.clock = clock;
    }

    public void record(AuthenticatedUser actor, AuditEntityType entityType, UUID entityId,
                       AuditAction action, List<FieldChange> changes) {
        record(actor.companyId(), actor.id(), entityType, entityId, action, changes);
    }

    public void record(UUID companyId, UUID actorId, AuditEntityType entityType, UUID entityId,
                       AuditAction action, List<FieldChange> changes) {
        if (action == AuditAction.UPDATED && changes.isEmpty()) {
            return;
        }
        logs.save(new AuditLog(
                companies.getReferenceById(companyId),
                users.getReferenceById(actorId),
                entityType, entityId, action, changes, Instant.now(clock)));
    }
}
