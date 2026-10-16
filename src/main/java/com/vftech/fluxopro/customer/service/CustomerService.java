package com.vftech.fluxopro.customer.service;

import com.vftech.fluxopro.company.entity.Company;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.customer.dto.CustomerRequest;
import com.vftech.fluxopro.customer.dto.CustomerResponse;
import com.vftech.fluxopro.customer.entity.Customer;
import com.vftech.fluxopro.customer.reposiroty.CustomerRepository;
import com.vftech.fluxopro.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final CompanyRepository companies;

    public CustomerService(CustomerRepository customers, CompanyRepository companies) {
        this.customers = customers;
        this.companies = companies;
    }

    @Transactional
    public CustomerResponse create(AuthenticatedUser user, CustomerRequest request) {
        Company company = companies.getReferenceById(user.companyId());
        Customer customer = customers.save(new Customer(
                company,
                request.name().trim(),
                blankToNull(request.email()),
                blankToNull(request.phone())));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(AuthenticatedUser user) {
        return customers.findByCompanyIdOrderByNameAsc(user.companyId()).stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(AuthenticatedUser user, UUID id) {
        return customers.findByIdAndCompanyId(id, user.companyId())
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
