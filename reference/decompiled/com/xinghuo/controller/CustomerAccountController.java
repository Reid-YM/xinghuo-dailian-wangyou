/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.CustomerAccountController
 *  com.xinghuo.dto.CustomerDtos$CustomerDashboardResponse
 *  com.xinghuo.dto.CustomerDtos$CustomerLoginRequest
 *  com.xinghuo.dto.CustomerDtos$CustomerProfileResponse
 *  com.xinghuo.dto.CustomerDtos$CustomerRegisterRequest
 *  com.xinghuo.service.CustomerAccountService
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.http.ResponseEntity
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.xinghuo.controller;

import com.xinghuo.dto.CustomerDtos;
import com.xinghuo.service.CustomerAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Generated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/account"})
public class CustomerAccountController {
    private final CustomerAccountService accounts;

    @PostMapping(value={"/register"})
    CustomerDtos.CustomerProfileResponse register(@Valid @RequestBody CustomerDtos.CustomerRegisterRequest body, HttpServletRequest req) {
        return this.accounts.register(body, req);
    }

    @PostMapping(value={"/login"})
    CustomerDtos.CustomerProfileResponse login(@Valid @RequestBody CustomerDtos.CustomerLoginRequest body, HttpServletRequest req) {
        return this.accounts.login(body, req);
    }

    @PostMapping(value={"/logout"})
    ResponseEntity<Void> logout(HttpServletRequest req) {
        this.accounts.logout(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value={"/me"})
    ResponseEntity<CustomerDtos.CustomerProfileResponse> me(HttpServletRequest req) {
        return this.accounts.current(req).map(x -> ResponseEntity.ok((Object)this.accounts.profile(x))).orElseGet(() -> ResponseEntity.status((int)401).build());
    }

    @GetMapping(value={"/dashboard"})
    ResponseEntity<CustomerDtos.CustomerDashboardResponse> dashboard(HttpServletRequest req) {
        return this.accounts.dashboard(req).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status((int)401).build());
    }

    @Generated
    public CustomerAccountController(CustomerAccountService accounts) {
        this.accounts = accounts;
    }
}

