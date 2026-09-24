package com.freight.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freight.common.exception.BusinessException;
import com.freight.service.IntelinkQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class IntelinkQuoteServiceImpl implements IntelinkQuoteService {

    private static final String VERSION = "1.0";
    private static final String NONCE = "slnkda";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();

    @Value("${intelink.base-url:https://openapi.intelink.net.cn}")
    private String baseUrl;
    @Value("${intelink.app-key:}")
    private String appKey;
    @Value("${intelink.app-secret:}")
    private String appSecret;

    @Override
    public JsonNode products(String businessBigType) {
        return request("/ois/order/getProductList", Map.of(
                "businessBigType", StringUtils.hasText(businessBigType) ? businessBigType : ""
        ));
    }

    @Override
    public JsonNode quote(Map<String, Object> request) {
        return request("/tms/expose/queryQuoteList", request == null ? Map.of() : request);
    }

    private JsonNode request(String path, Map<String, Object> params) {
        if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
            throw new BusinessException("天富行 OIS 接口未配置 appKey/appSecret");
        }
        try {
            return doRequest(path, params, false);
        } catch (RemoteApiException ex) {
            if (ex.shouldRefreshToken()) {
                tokenCache.set(null);
                try {
                    return doRequest(path, params, true);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("天富行报价接口请求被中断");
                } catch (BusinessException business) {
                    throw business;
                } catch (Exception retryError) {
                    throw new BusinessException("天富行报价接口重试失败：" + retryError.getMessage());
                }
            }
            throw new BusinessException("天富行报价接口调用失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException("天富行报价接口请求被中断");
        } catch (Exception ex) {
            throw new BusinessException("天富行报价接口请求失败：" + ex.getMessage());
        }
    }

    private JsonNode doRequest(String path, Map<String, Object> params, boolean forceRefresh)
            throws Exception {
        TokenContext tokenContext = getToken(forceRefresh);
        String tokenHeader = disguiseToken(tokenContext);
        Map<String, Object> requestParams = "/tms/expose/queryQuoteList".equals(path)
                ? Map.of("body1", objectMapper.writeValueAsString(params)) : params;
        String form = formBody(requestParams);
        String sign = sign(requestParams, tokenContext);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimBaseUrl() + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("version", VERSION)
                .header("token", tokenHeader)
                .header("sign", sign)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RemoteApiException("HTTP " + response.statusCode(), false);
        }
        JsonNode root = objectMapper.readTree(response.body());
        int resultCode = root.path("result_code").asInt(0);
        String code = root.path("code").asText("");
        if (resultCode == 1004 || resultCode == 1005 || "401".equals(code)) {
            throw new RemoteApiException(firstMessage(root, "授权 token 已失效"), true);
        }
        if (resultCode != 0 || (!code.isEmpty() && !"0".equals(code))) {
            throw new RemoteApiException(firstMessage(root, "远端返回错误"), false);
        }
        return root;
    }

    private TokenContext getToken(boolean forceRefresh) throws Exception {
        TokenCache cached = tokenCache.get();
        if (!forceRefresh && cached != null && cached.expiresAt > System.currentTimeMillis()) {
            return new TokenContext(cached.rawToken, System.currentTimeMillis(), NONCE);
        }
        String form = formBody(Map.of("appKey", appKey, "appSecret", appSecret));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimBaseUrl() + "/ois/order/getAuth"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode body = root.path("body");
        if (!"true".equalsIgnoreCase(body.path("ack").asText()) || !StringUtils.hasText(body.path("token").asText())) {
            throw new BusinessException("天富行 OIS 授权失败：" + firstMessage(root, "账号信息无效"));
        }
        // 官方示例要求把授权接口返回的 token 原值整体放入签名和伪装 token，不能拆解。
        String token = body.path("token").asText();
        TokenContext context = new TokenContext(token, System.currentTimeMillis(), NONCE);
        tokenCache.set(new TokenCache(token, System.currentTimeMillis() + 3 * 60 * 60 * 1000));
        return context;
    }

    private String sign(Map<String, Object> params, TokenContext tokenContext) throws Exception {
        List<String> pairs = new ArrayList<>();
        flatten(params, pairs);
        pairs.add("timestamp=" + tokenContext.timestamp);
        pairs.add("nonce=" + tokenContext.nonce);
        pairs.add("token=" + tokenContext.rawToken);
        pairs.add("version=" + VERSION);
        Collections.sort(pairs);
        String plain = String.join("&", pairs);
        String encoded = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest((encoded + appSecret).getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(32);
        for (byte value : digest) result.append(String.format("%02x", value));
        return result.toString().toUpperCase();
    }

    private void flatten(Map<String, Object> params, List<String> pairs) throws Exception {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null || "".equals(value)) continue;
            if (value instanceof Iterable<?> values) {
                for (Object item : values) {
                    if (item == null || "".equals(item)) continue;
                    pairs.add(entry.getKey() + "=" + scalarValue(item));
                }
            } else if (value.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(value, i);
                    if (item != null && !"".equals(item)) pairs.add(entry.getKey() + "=" + scalarValue(item));
                }
            } else if (value instanceof Map<?, ?> || value instanceof JsonNode) {
                pairs.add(entry.getKey() + "=" + objectMapper.writeValueAsString(value));
            } else {
                pairs.add(entry.getKey() + "=" + value);
            }
        }
    }

    private String scalarValue(Object value) throws Exception {
        return value instanceof Map<?, ?> || value instanceof JsonNode
                ? objectMapper.writeValueAsString(value) : String.valueOf(value);
    }

    private String formBody(Map<String, Object> params) throws Exception {
        List<String> fields = new ArrayList<>();
        flatten(params, fields);
        List<String> encoded = new ArrayList<>();
        for (String field : fields) {
            int split = field.indexOf('=');
            encoded.add(encode(field.substring(0, split)) + "=" + encode(field.substring(split + 1)));
        }
        return String.join("&", encoded);
    }

    private String disguiseToken(TokenContext tokenContext) throws Exception {
        var node = objectMapper.createObjectNode()
                .put("timestamp", tokenContext.timestamp)
                .put("nonce", tokenContext.nonce)
                .put("token", tokenContext.rawToken);
        String json = node.toString();
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8))
                .replace("a", "-").replace("c", "#").replace("x", "^").replace("M", "$");
    }

    private String trimBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String firstMessage(JsonNode root, String fallback) {
        for (String field : List.of("message", "msg", "solution")) {
            if (StringUtils.hasText(root.path(field).asText())) return root.path(field).asText();
        }
        return fallback;
    }

    private record TokenContext(String rawToken, long timestamp, String nonce) { }

    private record TokenCache(String rawToken, long expiresAt) { }

    private static class RemoteApiException extends RuntimeException {
        private final boolean refreshToken;

        RemoteApiException(String message, boolean refreshToken) {
            super(message);
            this.refreshToken = refreshToken;
        }

        boolean shouldRefreshToken() { return refreshToken; }
    }
}
