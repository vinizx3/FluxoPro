package com.vftech.fluxopro.notification.repository;

import com.vftech.fluxopro.notification.entity.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SentEmailRepository extends JpaRepository<SentEmail, UUID> {
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
}