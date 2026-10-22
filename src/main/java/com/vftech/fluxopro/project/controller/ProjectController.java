package com.vftech.fluxopro.project.controller;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.common.security.CurrentUser;
import com.vftech.fluxopro.project.dto.ProjectCreateRequest;
import com.vftech.fluxopro.project.dto.ProjectResponse;
import com.vftech.fluxopro.project.dto.ProjectStatusRequest;
import com.vftech.fluxopro.project.dto.ProjectUpdateRequest;
import com.vftech.fluxopro.project.entity.ProjectStatus;
import com.vftech.fluxopro.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService service;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return service.create(currentUser.get(), request);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(currentUser.get(), status, page, size);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable UUID id) {
        return service.get(currentUser.get(), id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody ProjectUpdateRequest request) {
        return service.update(currentUser.get(), id, request);
    }

    @PatchMapping("/{id}/status")
    public ProjectResponse move(@PathVariable UUID id, @Valid @RequestBody ProjectStatusRequest request) {
        return service.move(currentUser.get(), id, request);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        service.archive(currentUser.get(), id);
    }
}
