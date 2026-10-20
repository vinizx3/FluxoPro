package com.vftech.fluxopro.customer;

import com.vftech.fluxopro.common.exception.BusinessRuleException;
import com.vftech.fluxopro.common.exception.ConflictException;
import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.dto.CustomerResponse;
import com.vftech.fluxopro.customer.dto.CustomerUpdateRequest;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.repository.CustomerRepository;
import com.vftech.fluxopro.customer.service.CustomerService;
import com.vftech.fluxopro.opportunity.repository.OpportunityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Mock
    CustomerRepository customers;
    @Mock
    CompanyRepository companies;
    @Mock
    OpportunityRepository opportunities;

    private final UUID companyId = UUID.randomUUID();
    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customers, companies, opportunities, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(UUID.randomUUID(), companyId, com.vftech.fluxopro.user.entity.Role.OWNER);
    }

    private Customer customerWithVersion(long version) {
        Customer customer = new Customer(new Company("Acme"), "Client", null, null);
        ReflectionTestUtils.setField(customer, "version", version);
        return customer;
    }

    @Test
    void updateChangesFieldsWhenVersionMatches() {
        UUID id = UUID.randomUUID();
        when(customers.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(customerWithVersion(3L)));

        CustomerResponse response = service.update(user(), id,
                new CustomerUpdateRequest(" New name ", "new@acme.com", " ", 3L));

        assertThat(response.name()).isEqualTo("New name");
        assertThat(response.phone()).isNull();
        verify(customers).saveAndFlush(any(Customer.class));
    }

    @Test
    void updateWithStaleVersionIsRejectedAndChangesNothing() {
        UUID id = UUID.randomUUID();
        Customer customer = customerWithVersion(4L);
        when(customers.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.update(user(), id,
                new CustomerUpdateRequest("New name", null, null, 3L)))
                .isInstanceOf(ConflictException.class);

        assertThat(customer.getName()).isEqualTo("Client");
        verify(customers, never()).saveAndFlush(any());
    }

    @Test
    void archiveIsRejectedWhileCustomerHasOpenOpportunities() {
        UUID id = UUID.randomUUID();
        Customer customer = customerWithVersion(0L);
        when(customers.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(customer));
        when(opportunities.existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
                eq(id), eq(companyId), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.archive(user(), id))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(customer.isArchived()).isFalse();
    }

    @Test
    void archiveStampsTheDateWithTheClock() {
        UUID id = UUID.randomUUID();
        Customer customer = customerWithVersion(0L);
        when(customers.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.of(customer));
        when(opportunities.existsByCustomerIdAndCompanyIdAndArchivedAtIsNullAndStatusIn(
                eq(id), eq(companyId), anyCollection())).thenReturn(false);

        service.archive(user(), id);

        assertThat(customer.getArchivedAt()).isEqualTo(NOW);
    }

    @Test
    void customerOfAnotherCompanyIsNotFound() {
        UUID id = UUID.randomUUID();
        when(customers.findByIdAndCompanyId(id, companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(user(), id))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listClampsPageSizeAndNegativePage() {
        when(customers.findByCompanyIdAndArchivedAtIsNull(eq(companyId), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.list(user(), null, -5, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customers).findByCompanyIdAndArchivedAtIsNull(eq(companyId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listTrimsTheSearchTerm() {
        when(customers.findByCompanyIdAndArchivedAtIsNullAndNameContainingIgnoreCase(
                eq(companyId), eq("acme"), any(Pageable.class))).thenReturn(Page.empty());

        service.list(user(), "  acme ", 0, 20);

        verify(customers).findByCompanyIdAndArchivedAtIsNullAndNameContainingIgnoreCase(
                eq(companyId), eq("acme"), any(Pageable.class));
    }
}
