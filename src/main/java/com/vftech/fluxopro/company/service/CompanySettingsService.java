package com.vftech.fluxopro.company.service;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.dto.CompanySettingsResponse;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanySettingsService {

    private final CompanyRepository companies;

    public CompanySettingsService(CompanyRepository companies) {
        this.companies = companies;
    }

    @Transactional(readOnly = true)
    public CompanySettingsResponse get(AuthenticatedUser user) {
        return new CompanySettingsResponse(load(user).getStalledAfterDays());
    }

    @Transactional
    public CompanySettingsResponse update(AuthenticatedUser user, int stalledAfterDays) {
        if (!user.isOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can change settings");
        }
        if (stalledAfterDays < 1 || stalledAfterDays > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stalledAfterDays must be between 1 and 90");
        }
        Company company = load(user);
        company.changeStalledAfterDays(stalledAfterDays);
        return new CompanySettingsResponse(company.getStalledAfterDays());
    }

    private Company load(AuthenticatedUser user) {
        return companies.findById(user.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }
}
