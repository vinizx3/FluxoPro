package com.vftech.fluxopro.company.controller;

import com.vftech.fluxopro.common.security.CurrentUser;
import com.vftech.fluxopro.company.dto.CompanySettingsResponse;
import com.vftech.fluxopro.company.dto.UpdateCompanySettingsRequest;
import com.vftech.fluxopro.company.service.CompanySettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company/settings")
public class CompanySettingsController {

    private final CompanySettingsService service;
    private final CurrentUser currentUser;

    public CompanySettingsController(CompanySettingsService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public CompanySettingsResponse get() {
        return service.get(currentUser.get());
    }

    @PutMapping
    public CompanySettingsResponse update(@RequestBody UpdateCompanySettingsRequest request) {
        return service.update(currentUser.get(), request.stalledAfterDays());
    }
}