package com.vftech.fluxopro.customer.controller;

import com.vftech.fluxopro.customer.dto.CustomerRequest;
import com.vftech.fluxopro.customer.dto.CustomerResponse;
import com.vftech.fluxopro.customer.service.CustomerService;
import com.vftech.fluxopro.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService service;
    private final CurrentUser currentUser;

    public CustomerController(CustomerService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        return service.create(currentUser.get(), request);
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return service.list(currentUser.get());
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(currentUser.get(), id);
    }
}
