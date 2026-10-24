package com.vftech.fluxopro.notification;

import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.notification.service.StalledAlertJob;
import com.vftech.fluxopro.notification.service.StalledAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StalledAlertJobTest {

    @Mock
    CompanyRepository companies;
    @Mock
    StalledAlertService alerts;

    @Test
    void oneCompanyFailingDoesNotStopTheOthers() {
        UUID failing = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        when(companies.findAllIds()).thenReturn(List.of(failing, healthy));
        when(alerts.createAlerts(failing, 7)).thenThrow(new IllegalStateException("boom"));
        when(alerts.createAlerts(healthy, 7)).thenReturn(2);

        new StalledAlertJob(companies, alerts, 7).run();

        verify(alerts).createAlerts(failing, 7);
        verify(alerts).createAlerts(healthy, 7);
    }
}
