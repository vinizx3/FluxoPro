package com.vftech.fluxopro.company;

import com.vftech.fluxopro.audit.entity.AuditAction;
import com.vftech.fluxopro.audit.entity.AuditEntityType;
import com.vftech.fluxopro.audit.entity.FieldChange;
import com.vftech.fluxopro.audit.service.AuditRecorder;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.dto.CompanySettingsResponse;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.company.service.CompanySettingsService;
import com.vftech.fluxopro.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanySettingsServiceTest {

    @Mock
    CompanyRepository companies;
    @Mock
    AuditRecorder audit;

    private final UUID companyId = UUID.randomUUID();

    private CompanySettingsService service() {
        return new CompanySettingsService(companies, audit);
    }

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    private AuthenticatedUser seller() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.SELLER);
    }

    @Test
    void anyoneInTheCompanyCanReadTheThreshold() {
        when(companies.findById(companyId)).thenReturn(Optional.of(new Company("Acme")));

        assertThat(service().get(seller()).stalledAfterDays()).isEqualTo(7);
    }

    @Test
    void theOwnerCanChangeTheThreshold() {
        Company company = new Company("Acme");
        when(companies.findById(companyId)).thenReturn(Optional.of(company));

        CompanySettingsResponse response = service().update(owner(), 14);

        assertThat(response.stalledAfterDays()).isEqualTo(14);
        assertThat(company.getStalledAfterDays()).isEqualTo(14);
    }

    @Test
    void theChangeIsRecordedInTheAuditLog() {
        when(companies.findById(companyId)).thenReturn(Optional.of(new Company("Acme")));
        AuthenticatedUser owner = owner();

        service().update(owner, 14);

        verify(audit).record(eq(owner), eq(AuditEntityType.COMPANY), any(), eq(AuditAction.UPDATED),
                eq(List.of(new FieldChange("stalledAfterDays", "7", "14"))));
    }

    @Test
    void savingTheSameValueRecordsNoChanges() {
        when(companies.findById(companyId)).thenReturn(Optional.of(new Company("Acme")));
        AuthenticatedUser owner = owner();

        service().update(owner, 7);

        verify(audit).record(eq(owner), eq(AuditEntityType.COMPANY), any(), eq(AuditAction.UPDATED),
                eq(List.of()));
    }

    @Test
    void aSellerCannotChangeTheThreshold() {
        assertThatThrownBy(() -> service().update(seller(), 14))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(companies, audit);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 91})
    void valuesOutsideTheRangeAreRejected(int days) {
        assertThatThrownBy(() -> service().update(owner(), days))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(companies, audit);
    }
}
