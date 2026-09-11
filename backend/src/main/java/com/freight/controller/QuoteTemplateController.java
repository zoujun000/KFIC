package com.freight.controller;

import com.freight.common.result.Result;
import com.freight.service.QuoteTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "报价模版")
@RestController
@RequestMapping("/api/quote-template")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class QuoteTemplateController {

    private final QuoteTemplateService quoteTemplateService;

    @Operation(summary = "获取当前用户报价模版")
    @GetMapping
    public Result<Map<String, String>> get() {
        String template = quoteTemplateService.getCurrentUserTemplate();
        return Result.success(Map.of("template", template == null ? "" : template));
    }

    @Operation(summary = "保存当前用户报价模版")
    @PutMapping
    public Result<Void> save(@RequestBody Map<String, String> body) {
        quoteTemplateService.saveCurrentUserTemplate(body.get("template"));
        return Result.success();
    }
}
