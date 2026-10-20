package com.vftech.fluxopro.customer.dto;

import com.vftech.fluxopro.customer.entity.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(UUID id, String name, String email, String phone, Instant createdAt, Instant archivedAt, Long version) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCreatedAt(),
                customer.getArchivedAt(),
                customer.getVersion());
    }
}
