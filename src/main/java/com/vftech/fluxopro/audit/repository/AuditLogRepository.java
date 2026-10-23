package com.vftech.fluxopro.audit.repository;

import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findByCompanyId(UUID companyId, Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findByCompanyIdAndEntityTypeAndEntityId(
            UUID companyId, AuditEntityType entityType, UUID entityId, Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    Page<AuditLog> findByCompanyIdAndEntityType(UUID companyId, AuditEntityType entityType, Pageable pageable);
}
