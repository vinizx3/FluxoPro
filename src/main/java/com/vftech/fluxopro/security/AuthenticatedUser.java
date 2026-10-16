package com.vftech.fluxopro.security;

import com.vftech.fluxopro.user.entity.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID id, UUID companyId, Role role) {

    public boolean isOwner(){
        return role == Role.OWNER;
    }
}
