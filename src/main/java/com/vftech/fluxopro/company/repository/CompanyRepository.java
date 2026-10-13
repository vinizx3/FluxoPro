package com.vftech.fluxopro.company.repository;

import com.vftech.fluxopro.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}