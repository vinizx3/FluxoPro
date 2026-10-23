package com.vftech.fluxopro.audit;

import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.repository.AuditLogRepository;
import com.vftech.fluxopro.audit.service.AuditService;
import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    AuditLogRepository logs;

    private final UUID companyId = UUID.randomUUID();
    private AuditService service;

    @BeforeEach
    void setUp() {
        service = new AuditService(logs);
    }

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    @Test
    void sellerCannotReadTheAuditLog() {
        AuthenticatedUser seller = new AuthenticatedUser(UUID.randomUUID(), companyId, Role.SELLER);

        assertThatThrownBy(() -> service.list(seller, null, null, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only owners");
        verifyNoInteractions(logs);
    }

    @Test
    void entityIdWithoutEntityTypeIsRejected() {
        assertThatThrownBy(() -> service.list(owner(), null, UUID.randomUUID(), 0, 20))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void withoutFiltersItListsTheWholeCompanyAndClampsThePage() {
        when(logs.findByCompanyId(eq(companyId), any(Pageable.class))).thenReturn(Page.empty());

        service.list(owner(), null, null, -3, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(logs).findByCompanyId(eq(companyId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void filtersByEntityType() {
        when(logs.findByCompanyIdAndEntityType(eq(companyId), eq(AuditEntityType.PROJECT), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(owner(), AuditEntityType.PROJECT, null, 0, 20);

        verify(logs).findByCompanyIdAndEntityType(eq(companyId), eq(AuditEntityType.PROJECT), any(Pageable.class));
    }

    @Test
    void filtersByOneEntity() {
        UUID entityId = UUID.randomUUID();
        when(logs.findByCompanyIdAndEntityTypeAndEntityId(
                eq(companyId), eq(AuditEntityType.OPPORTUNITY), eq(entityId), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(owner(), AuditEntityType.OPPORTUNITY, entityId, 0, 20);

        verify(logs).findByCompanyIdAndEntityTypeAndEntityId(
                eq(companyId), eq(AuditEntityType.OPPORTUNITY), eq(entityId), any(Pageable.class));
    }
}
