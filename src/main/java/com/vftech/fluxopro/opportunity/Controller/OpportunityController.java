package com.vftech.fluxopro.opportunity.Controller;

import com.vftech.fluxopro.common.dto.PageResponse;
import com.vftech.fluxopro.opportunity.dto.*;
import com.vftech.fluxopro.opportunity.entity.OpportunityStatus;
import com.vftech.fluxopro.opportunity.service.OpportunityService;
import com.vftech.fluxopro.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/opportunities")
public class OpportunityController {

    private final OpportunityService service;
    private final CurrentUser currentUser;

    public OpportunityController(OpportunityService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpportunityResponse create(@Valid @RequestBody OpportunityRequest request) {
        return service.create(currentUser.get(), request);
    }

    @GetMapping("/board")
    public Map<OpportunityStatus, List<OpportunityResponse>> board() {
        return service.board(currentUser.get());
    }

    @GetMapping("/{id}")
    public OpportunityResponse get(@PathVariable UUID id) {
        return service.get(currentUser.get(), id);
    }

    @GetMapping("/stalled")
    public PageResponse<StalledOpportunityResponse> stalled(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.stalled(currentUser.get(), days, page, size);
    }

    @PutMapping("/{id}")
    public OpportunityResponse update(@PathVariable UUID id, @Valid @RequestBody OpportunityUpdateRequest request) {
        return service.update(currentUser.get(), id, request);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable UUID id) {
        service.archive(currentUser.get(), id);
    }

    @PatchMapping("/{id}/status")
    public OpportunityResponse move(@PathVariable UUID id, @Valid @RequestBody MoveStatusRequest request) {
        return service.move(currentUser.get(), id, request);
    }
}
