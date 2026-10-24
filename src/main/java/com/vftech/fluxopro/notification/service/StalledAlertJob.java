package com.vftech.fluxopro.notification.service;

import com.vftech.fluxopro.company.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StalledAlertJob {

    private static final Logger log = LoggerFactory.getLogger(StalledAlertJob.class);

    private final CompanyRepository companies;
    private final StalledAlertService alerts;
    private final int stalledAfterDays;

    public StalledAlertJob(CompanyRepository companies, StalledAlertService alerts,
                           @Value("${app.alerts.stalled-after-days:7}") int stalledAfterDays) {
        this.companies = companies;
        this.alerts = alerts;
        this.stalledAfterDays = stalledAfterDays;
    }

    @Scheduled(cron = "${app.alerts.cron:0 0 8 * * *}", zone = "America/Sao_Paulo")
    public void run() {
        int total = 0;
        for (UUID companyId : companies.findAllIds()) {
            try {
                total += alerts.createAlerts(companyId, stalledAfterDays);
            } catch (RuntimeException e) {
                // One company failing must not stop the others.
                log.error("Stalled alerts failed for company {}", companyId, e);
            }
        }
        log.info("Stalled alerts job finished: {} new notifications", total);
    }
}
