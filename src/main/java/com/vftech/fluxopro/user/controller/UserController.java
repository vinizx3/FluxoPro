package com.vftech.fluxopro.user.controller;

import com.vftech.fluxopro.common.security.CurrentUser;
import com.vftech.fluxopro.user.dto.CreateSellerRequest;
import com.vftech.fluxopro.user.dto.UserResponse;
import com.vftech.fluxopro.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    public UserController(UserService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createSeller(@Valid @RequestBody CreateSellerRequest request) {
        return service.createSeller(currentUser.get(), request);
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.list(currentUser.get());
    }

    @PostMapping("/{id}/activate")
    public UserResponse activate(@PathVariable UUID id) {
        return service.setActive(currentUser.get(), id, true);
    }

    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id) {
        return service.setActive(currentUser.get(), id, false);
    }
}
