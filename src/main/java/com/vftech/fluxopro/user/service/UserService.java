package com.vftech.fluxopro.user.service;

import com.vftech.fluxopro.common.security.AuthenticatedUser;
import com.vftech.fluxopro.company.repository.CompanyRepository;
import com.vftech.fluxopro.user.dto.CreateSellerRequest;
import com.vftech.fluxopro.user.dto.UserResponse;
import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;
import com.vftech.fluxopro.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, CompanyRepository companies, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.companies = companies;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createSeller(AuthenticatedUser actor, CreateSellerRequest request) {
        requireOwner(actor);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User seller = users.save(new User(
                companies.getReferenceById(actor.companyId()),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.SELLER));
        return UserResponse.from(seller);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(AuthenticatedUser actor) {
        requireOwner(actor);
        return users.findByCompanyIdOrderByNameAsc(actor.companyId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse setActive(AuthenticatedUser actor, UUID id, boolean active) {
        requireOwner(actor);
        User user = users.findByIdAndCompanyId(id, actor.companyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "The owner cannot be deactivated");
        }
        user.setActive(active);
        return UserResponse.from(user);
    }

    private void requireOwner(AuthenticatedUser actor) {
        if (!actor.isOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can manage users");
        }
    }
}
