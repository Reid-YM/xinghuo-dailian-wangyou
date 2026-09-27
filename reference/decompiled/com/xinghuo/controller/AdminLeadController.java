/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.xinghuo.controller.AdminLeadController
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteAiSuggestionResponse
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteCrmUpdateRequest
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteLeadPageResponse
 *  com.xinghuo.dto.QuoteLeadDtos$QuoteLeadResponse
 *  com.xinghuo.service.QuoteService
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.validation.Valid
 *  lombok.Generated
 *  org.springframework.security.core.Authentication
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.xinghuo.controller;

import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.service.QuoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.Generated;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/admin/leads"})
public class AdminLeadController {
    private final QuoteService quotes;

    @GetMapping
    QuoteLeadDtos.QuoteLeadPageResponse leads(@RequestParam(value="q", required=false) String q, @RequestParam(value="status", required=false) String status, @RequestParam(value="intent", required=false) String intent, @RequestParam(value="source", required=false) String source, @RequestParam(value="page", required=false, defaultValue="0") int page, @RequestParam(value="size", required=false, defaultValue="20") int size) {
        return this.quotes.listLeads(q, status, intent, source, page, size);
    }

    @PatchMapping(value={"/{id}/crm"})
    QuoteLeadDtos.QuoteLeadResponse updateCrm(@PathVariable UUID id, @Valid @RequestBody QuoteLeadDtos.QuoteCrmUpdateRequest r, Authentication a, HttpServletRequest req) {
        return this.quotes.updateCrm(id, r, a == null ? "admin" : a.getName(), req);
    }

    @PostMapping(value={"/{id}/ai-suggestion"})
    QuoteLeadDtos.QuoteAiSuggestionResponse aiSuggestion(@PathVariable UUID id, Authentication a, HttpServletRequest req) {
        return this.quotes.generateLeadSuggestion(id, a == null ? "admin" : a.getName(), req);
    }

    @Generated
    public AdminLeadController(QuoteService quotes) {
        this.quotes = quotes;
    }
}

