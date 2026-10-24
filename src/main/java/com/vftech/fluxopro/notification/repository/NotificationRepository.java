package com.vftech.fluxopro.notification.repository;

import com.vftech.fluxopro.notification.entity.Notification;
import com.vftech.fluxopro.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByOpportunityIdAndTypeAndStalledSince(
            UUID opportunityId, NotificationType type, Instant stalledSince);

    @EntityGraph(attributePaths = {"opportunity", "opportunity.customer"})
    Page<Notification> findByCompanyIdAndRecipientId(UUID companyId, UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"opportunity", "opportunity.customer"})
    Page<Notification> findByCompanyIdAndRecipientIdAndReadAtIsNull(
            UUID companyId, UUID userId, Pageable pageable);

    long countByCompanyIdAndRecipientIdAndReadAtIsNull(UUID companyId, UUID userId);

    @EntityGraph(attributePaths = {"opportunity", "opportunity.customer"})
    Optional<Notification> findByIdAndCompanyIdAndRecipientId(UUID id, UUID companyId, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Notification n set n.readAt = :now
            where n.company.id = :companyId and n.recipient.id = :userId and n.readAt is null
            """)
    int markAllRead(@Param("companyId") UUID companyId, @Param("userId") UUID userId,
                    @Param("now") Instant now);
}
