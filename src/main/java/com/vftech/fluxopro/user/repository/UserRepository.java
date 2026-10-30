package com.vftech.fluxopro.user.repository;

import com.vftech.fluxopro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByCompanyIdOrderByNameAsc(UUID companyId);
    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);
}