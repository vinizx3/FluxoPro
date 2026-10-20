package com.vftech.fluxopro.customer.controller;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.customer.dto.CustomerRequest;
import com.vftech.fluxopro.customer.dto.CustomerResponse;
import com.vftech.fluxopro.customer.dto.CustomerUpdateRequest;
import com.vftech.fluxopro.customer.service.CustomerService;
import com.vftech.fluxopro.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    public PageResponse<CustomerResponse> list(@RequestParam(required = false) String search,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return service.list(currentUser.get(), search, page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return service.get(currentUser.get(), id);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
        return service.update(currentUser.get(), id, request);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        service.archive(currentUser.get(), id);
    }
}
