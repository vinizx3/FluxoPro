package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.company.dto.CompanyThreshold;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StalledAlertJob {

    private static final Logger log = LoggerFactory.getLogger(StalledAlertJob.class);

    private final CompanyRepository companies;
    private final StalledAlertService alerts;

    public StalledAlertJob(CompanyRepository companies, StalledAlertService alerts) {
        this.companies = companies;
        this.alerts = alerts;
    }

    @Scheduled(cron = "${app.alerts.cron:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public void run() {
        int total = 0;
        for (CompanyThreshold company : companies.findAllProjectedBy()) {
            try {
                total += alerts.createAlerts(company.id(), company.stalledAfterDays());
            } catch (RuntimeException e) {
                // One company failing must not stop the others.
                log.error("Stalled alerts failed for company {}", company.id(), e);
            }
        }
        log.info("Stalled alerts job finished: {} new notifications", total);
    }
}
