package com.vftech.fluxopro.user.dto;

import com.vftech.fluxopro.user.entity.Role;
import com.vftech.fluxopro.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Role role, boolean active, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
