package com.freight.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freight.common.exception.BusinessException;
import com.freight.service.AisStreamService;
import com.freight.vo.ShipLocateSearchVO;
import com.freight.vo.ShipLocateStatusVO;
import com.freight.vo.ShipLocateVO;
import com.freight.vo.ShipPortCallVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Primary
@Service
@RequiredArgsConstructor
public class ShipxyServiceImpl implements AisStreamService {

    private static final String BASE_URL = "https://api.shipxy.com/apicall/v3";
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final Map<Integer, String> SHIP_TYPES = Map.ofEntries(
            Map.entry(30, "渔船"), Map.entry(40, "高速艇"), Map.entry(50, "引航船"),
            Map.entry(52, "拖轮"), Map.entry(60, "客轮"), Map.entry(70, "货轮"),
            Map.entry(71, "散货船"), Map.entry(72, "杂货船"), Map.entry(73, "集装箱船"),
            Map.entry(74, "冷藏船"), Map.entry(75, "滚装船"), Map.entry(76, "油轮"),
            Map.entry(77, "化学品船"), Map.entry(79, "液化气船"), Map.entry(90, "其他船舶")
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${shipxy.api-key:}")
    private String apiKey;

    @Override
    public List<ShipLocateSearchVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        JsonNode root = request("SearchShip", Map.of(
                "keywords", keyword.trim(),
                "max", String.valueOf(MAX_SEARCH_RESULTS)
        ));
        List<ShipLocateSearchVO> result = new ArrayList<>();
        for (JsonNode item : root.path("data")) {
            ShipLocateSearchVO vo = new ShipLocateSearchVO();
            vo.setMmsi(text(item, "mmsi"));
            vo.setName(text(item, "ship_name"));
            vo.setImo(text(item, "imo"));
            vo.setCallsign(text(item, "call_sign"));
            vo.setShipType(shipType(item.path("shiptype").asInt(-1)));
            vo.setLastUpdateAt(epochMillis(item.path("last_time_utc")));
            result.add(vo);
        }
        return result;
    }

    @Override
    public ShipLocateVO locate(String mmsi) {
        if (!StringUtils.hasText(mmsi) || !mmsi.matches("\\d{9}")) {
            return null;
        }
        JsonNode data = request("GetSingleShip", Map.of("mmsi", mmsi)).path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }

        ShipLocateVO vo = new ShipLocateVO();
        ShipLocateVO.ShipInfo info = new ShipLocateVO.ShipInfo();
        info.setMmsi(text(data, "mmsi"));
        info.setName(firstText(data, "ship_cnname", "ship_name"));
        info.setImo(text(data, "imo"));
        info.setCallsign(text(data, "call_sign"));
        info.setShipType(shipType(data.path("ship_type").asInt(-1)));
        info.setLength(number(data, "length"));
        info.setBreadth(number(data, "width"));
        info.setDraught(number(data, "draught"));
        info.setDestination(text(data, "dest"));
        info.setEta(text(data, "eta"));
        vo.setShip(info);

        if (data.has("lat") && data.has("lng")) {
            ShipLocateVO.LatestPosition latest = new ShipLocateVO.LatestPosition();
            latest.setLat(number(data, "lat"));
            latest.setLon(number(data, "lng"));
            latest.setSog(validNumber(data, "sog", -1));
            latest.setCog(validNumber(data, "cog", -1));
            latest.setHeading(validNumber(data, "hdg", 511));
            latest.setNavStatus(validInt(data, "navistat", -1));
            latest.setTime(epochMillis(data.path("last_time_utc")));
            vo.setLatest(latest);
        }
        vo.setTrack(List.of());
        vo.setUpdateAt(System.currentTimeMillis());
        return vo;
    }

    @Override
    public List<ShipPortCallVO> portCalls(String mmsi, int days) {
        if (!StringUtils.hasText(mmsi) || !mmsi.matches("\\d{9}")) {
            return List.of();
        }
        long endTime = Instant.now().getEpochSecond();
        long startTime = endTime - Duration.ofDays(days).toSeconds();
        JsonNode root = request("GetPortofCallByShip", Map.of(
                "mmsi", mmsi,
                "start_time", String.valueOf(startTime),
                "end_time", String.valueOf(endTime),
                "time_zone", "2"
        ));
        List<ShipPortCallVO> result = new ArrayList<>();
        for (JsonNode item : root.path("data")) {
            ShipPortCallVO vo = new ShipPortCallVO();
            vo.setPortName(firstText(item, "port_cnname", "port_name"));
            vo.setPortCode(text(item, "port_code"));
            vo.setCountryName(firstText(item, "port_country_cnname", "port_country_name"));
            vo.setTerminalName(text(item, "terminal_name"));
            vo.setBerthName(text(item, "berth_name"));
            vo.setArrivalAnchorage(text(item, "arrival_anchorage"));
            vo.setAta(text(item, "ata"));
            vo.setAtb(text(item, "atb"));
            vo.setAtd(text(item, "atd"));
            vo.setStayTime(number(item, "stay_time"));
            vo.setStayTerminalTime(firstNumber(item, "stay_terminal_time", "stay_interminal_time"));
            vo.setArrivalDraught(number(item, "arrival_draught"));
            vo.setDepartureDraught(number(item, "departure_draught"));
            result.add(vo);
        }
        return result;
    }

    @Override
    public ShipLocateStatusVO status() {
        ShipLocateStatusVO vo = new ShipLocateStatusVO();
        vo.setConnected(StringUtils.hasText(apiKey));
        return vo;
    }

    private JsonNode request(String endpoint, Map<String, String> params) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(503, "未配置 ShipXY API Key，请设置 SHIPXY_API_KEY");
        }
        StringBuilder url = new StringBuilder(BASE_URL).append('/').append(endpoint).append("?key=")
                .append(encode(apiKey));
        params.forEach((name, value) -> url.append('&').append(name).append('=').append(encode(value)));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new BusinessException(502, "ShipXY 服务暂时不可用");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("status").asInt(-1) != 0) {
                String message = root.path("msg").asText("ShipXY 查询失败");
                throw new BusinessException(502, "ShipXY 查询失败：" + message);
            }
            return root;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(502, "ShipXY 服务请求失败");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private static String firstText(JsonNode node, String first, String second) {
        String value = text(node, first);
        return value.isBlank() ? text(node, second) : value;
    }

    private static Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private static Double firstNumber(JsonNode node, String first, String second) {
        Double value = number(node, first);
        return value == null ? number(node, second) : value;
    }

    private static Double validNumber(JsonNode node, String field, double invalidValue) {
        Double value = number(node, field);
        return value == null || value == invalidValue ? null : value;
    }

    private static Integer validInt(JsonNode node, String field, int invalidValue) {
        JsonNode value = node.path(field);
        return value.isNumber() && value.asInt() != invalidValue ? value.asInt() : null;
    }

    private static Long epochMillis(JsonNode node) {
        return node.isNumber() && node.asLong() > 0 ? node.asLong() * 1000 : null;
    }

    private static String shipType(int code) {
        return code < 0 ? "" : SHIP_TYPES.getOrDefault(code, "类型 " + code);
    }
}
