package com.vftech.fluxopro.company;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySettingsServiceTest {

    @Mock
    CompanyRepository companyRepository;

    private final UUID companyId = UUID.randomUUID();

    private AuthenticatedUser owner() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.OWNER);
    }

    private AuthenticatedUser seller() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, Role.SELLER);
    }

    @Test
    void anyoneInTheCompanyCanReadTheThreshold() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company("Acme")));

        assertThat(new CompanySettingsService(companyRepository).get(seller()).stalledAfterDays()).isEqualTo(7);
    }

    @Test
    void theOwnerCanChangeTheThreshold() {
        Company company = new Company("Acme");
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        CompanySettingsResponse response = new CompanySettingsService(companyRepository).update(owner(), 14);

        assertThat(response.stalledAfterDays()).isEqualTo(14);
        assertThat(company.getStalledAfterDays()).isEqualTo(14);
    }

    @Test
    void aSellerCannotChangeTheThreshold() {
        assertThatThrownBy(() -> new CompanySettingsService(companyRepository).update(seller(), 14))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(companyRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 91})
    void valuesOutsideTheRangeAreRejected(int days) {
        assertThatThrownBy(() -> new CompanySettingsService(companyRepository).update(owner(), days))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(companyRepository);
    }
}
