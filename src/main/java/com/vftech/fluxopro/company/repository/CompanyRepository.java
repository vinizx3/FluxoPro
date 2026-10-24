package com.vftech.fluxopro.company.repository;

import com.vftech.fluxopro.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    @Query("select c.id from Company c")
    List<UUID> findAllIds();
}