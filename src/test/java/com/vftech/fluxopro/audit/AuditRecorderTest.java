package com.vftech.fluxopro.audit;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.AuditLog;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.repository.AuditLogRepository;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRecorderTest {

    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

    @Mock AuditLogRepository logs;
    @Mock CompanyRepository companies;
    @Mock UserRepository users;

    private final UUID companyId = UUID.randomUUID();
    private AuditRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AuditRecorder(logs, companies, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthenticatedUser actor() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    @Test
    void recordsWhoWhatAndWhenUsingTheClock() {
        AuthenticatedUser actor = actor();
        UUID entityId = UUID.randomUUID();
        when(companies.getReferenceById(companyId)).thenReturn(new Company("Acme"));
        when(users.getReferenceById(actor.id())).thenReturn(mock(User.class));

        recorder.record(actor, AuditEntityType.OPPORTUNITY, entityId, AuditAction.STATUS_CHANGED,
                List.of(new FieldChange("status", "LEAD", "QUALIFICATION")));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.OPPORTUNITY);
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getAction()).isEqualTo(AuditAction.STATUS_CHANGED);
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getChanges()).containsExactly(new FieldChange("status", "LEAD", "QUALIFICATION"));
    }

    @Test
    void updateWithoutChangesIsNotRecorded() {
        recorder.record(actor(), AuditEntityType.CUSTOMER, UUID.randomUUID(), AuditAction.UPDATED, List.of());

        verify(logs, never()).save(any());
    }

    @Test
    void createdIsRecordedEvenWithoutFieldChanges() {
        AuthenticatedUser actor = actor();
        when(companies.getReferenceById(companyId)).thenReturn(new Company("Acme"));
        when(users.getReferenceById(actor.id())).thenReturn(mock(User.class));

        recorder.record(actor, AuditEntityType.CUSTOMER, UUID.randomUUID(), AuditAction.CREATED, List.of());

        verify(logs).save(any(AuditLog.class));
    }
}
