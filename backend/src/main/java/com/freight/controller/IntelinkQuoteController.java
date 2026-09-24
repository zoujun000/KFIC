package com.freight.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.freight.common.result.Result;
import com.freight.service.IntelinkQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "天富行快递报价")
@RestController
@RequestMapping("/api/express-quotes")
@RequiredArgsConstructor
public class IntelinkQuoteController {

    private final IntelinkQuoteService quoteService;

    @Operation(summary = "获取快递/FBA渠道")
    @GetMapping("/products")
    public Result<JsonNode> products(@RequestParam(required = false) String businessBigType) {
        return Result.success(quoteService.products(businessBigType));
    }

    @Operation(summary = "查询天富行快递报价")
    @PostMapping("/query")
    public Result<JsonNode> query(@RequestBody Map<String, Object> request) {
        return Result.success(quoteService.quote(request));
    }
}
