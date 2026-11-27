package com.vftech.fluxopro.today.controller;

import com.vftech.fluxopro.common.security.CurrentUser;
import com.vftech.fluxopro.today.dto.TodayResponse;
import com.vftech.fluxopro.today.service.TodayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/today")
public class TodayController {

    private final TodayService service;
    private final CurrentUser currentUser;

    public TodayController(TodayService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public TodayResponse get() {
        return service.build(currentUser.get());
    }
}
