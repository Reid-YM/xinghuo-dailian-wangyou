package com.xinghuo.controller;

import com.xinghuo.dto.QuoteLeadDtos;
import com.xinghuo.quote.application.QuoteApplicationService;
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
    private final QuoteApplicationService quotes;

    @GetMapping
    QuoteLeadDtos.QuoteLeadPageResponse leads(@RequestParam(value="q", required=false) String q, @RequestParam(value="status", required=false) String status, @RequestParam(value="intent", required=false) String intent, @RequestParam(value="source", required=false) String source, @RequestParam(value="page", required=false, defaultValue="0") int page, @RequestParam(value="size", required=false, defaultValue="20") int size) {
        return this.quotes.listLeads(q, status, intent, source, page, size);
    }

    @PatchMapping(value={"/{id}/crm"})
    QuoteLeadDtos.QuoteLeadResponse updateCrm(@PathVariable UUID id, @Valid @RequestBody QuoteLeadDtos.QuoteCrmUpdateRequest r, Authentication a) {
        return this.quotes.updateCrm(id, r, a == null ? "admin" : a.getName());
    }

    @PostMapping(value={"/{id}/ai-suggestion"})
    QuoteLeadDtos.QuoteAiSuggestionResponse aiSuggestion(@PathVariable UUID id, Authentication a) {
        return this.quotes.generateLeadSuggestion(id, a == null ? "admin" : a.getName());
    }

    @Generated
    public AdminLeadController(QuoteApplicationService quotes) {
        this.quotes = quotes;
    }
}
