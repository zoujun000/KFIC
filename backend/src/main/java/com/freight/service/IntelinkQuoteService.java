package com.freight.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface IntelinkQuoteService {

    JsonNode products(String businessBigType);

    JsonNode quote(Map<String, Object> request);
}
