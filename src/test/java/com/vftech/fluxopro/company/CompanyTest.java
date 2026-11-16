package com.vftech.fluxopro.company;

import com.vftech.fluxopro.company.entity.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    @Test
    void newCompaniesStartWithSevenDays() {
        assertThat(new Company("Acme").getStalledAfterDays()).isEqualTo(7);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30, 90})
    void acceptsValuesInsideTheRange(int days) {
        Company company = new Company("Acme");
        company.changeStalledAfterDays(days);
        assertThat(company.getStalledAfterDays()).isEqualTo(days);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 91})
    void rejectsValuesOutsideTheRange(int days) {
        Company company = new Company("Acme");
        assertThatThrownBy(() -> company.changeStalledAfterDays(days))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(company.getStalledAfterDays()).isEqualTo(7);
    }
}