package com.vftech.fluxopro.customer.repository;

import com.vftech.fluxopro.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Customer> findByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<Customer> findByIdAndCompanyIdAndArchivedAtIsNull(UUID id, UUID companyId);

    Page<Customer> findByCompanyIdAndArchivedAtIsNull(UUID companyId, Pageable pageable);

    Page<Customer> findByCompanyIdAndArchivedAtIsNullAndNameContainingIgnoreCase(
            UUID companyId, String name, Pageable pageable);
}
