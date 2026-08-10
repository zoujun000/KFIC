package com.freight.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freight.service.AisStreamService;
import com.freight.vo.ShipLocateSearchVO;
import com.freight.vo.ShipLocateStatusVO;
import com.freight.vo.ShipLocateVO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AisStreamServiceImpl implements AisStreamService, WebSocket.Listener {

    private static final String STREAM_URL = "wss://stream.aisstream.io/v0/stream";
    private static final int MAX_REGISTRY_SHIPS = 100_000;
    private static final int MAX_ACTIVE_TRACKS = 1000;
    private static final int MAX_TRACK_POINTS = 300;
    private static final long TRACK_POINT_INTERVAL_MS = 60_000;
    private static final double TRACK_POINT_DISTANCE_DEG = 0.01;
    private static final int MAX_SEARCH_RESULTS = 20;

    private static final Map<Integer, String> SHIP_TYPE_TEXT = Map.ofEntries(
            Map.entry(30, "渔船"),
            Map.entry(31, "拖网渔船"),
            Map.entry(32, "围网渔船"),
            Map.entry(33, "拖带渔船"),
            Map.entry(35, "延绳钓渔船"),
            Map.entry(36, "加工渔船"),
            Map.entry(37, "捕捞渔船"),
            Map.entry(40, "高速艇"),
            Map.entry(41, "高速艇"),
            Map.entry(50, "引航船"),
            Map.entry(51, "搜救船"),
            Map.entry(52, "拖轮"),
            Map.entry(53, "港口供应船"),
            Map.entry(54, "污染控制船"),
            Map.entry(55, "执法船"),
            Map.entry(58, "医疗运输船"),
            Map.entry(59, "辅助船"),
            Map.entry(60, "客轮"),
            Map.entry(70, "货轮"),
            Map.entry(71, "散货船"),
            Map.entry(72, "杂货船"),
            Map.entry(73, "集装箱船"),
            Map.entry(74, "冷藏船"),
            Map.entry(75, "滚装船"),
            Map.entry(76, "油轮"),
            Map.entry(77, "化学品船"),
            Map.entry(78, "其他货船"),
            Map.entry(79, "液化气船"),
            Map.entry(80, "油轮"),
            Map.entry(90, "其他船舶")
    );

    private final ObjectMapper objectMapper;

    @Value("${aisstream.api-key}")
    private String apiKey;

    private final Map<String, ShipData> ships = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> nameIndex = new ConcurrentHashMap<>();
    private final Map<String, TrackData> tracks = new ConcurrentHashMap<>();
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "aisstream-client");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile long lastMessageAt;
    private int reconnectAttempt;
    private StringBuilder textBuffer = new StringBuilder();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduler.execute(this::connect);
    }

    private synchronized void connect() {
        if (connecting.get() || webSocket != null) {
            return;
        }
        connecting.set(true);
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(STREAM_URL), this)
                .whenComplete((socket, error) -> {
                    connecting.set(false);
                    if (error != null) {
                        log.warn("AISStream 连接失败: {}", error.getMessage());
                        scheduleReconnect();
                    }
                });
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        this.connected = true;
        this.reconnectAttempt = 0;
        this.textBuffer = new StringBuilder();
        log.info("AISStream 已连接");
        sendSubscription(webSocket);
        webSocket.request(1);
    }

    private void sendSubscription(WebSocket webSocket) {
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("APIKey", apiKey);
            message.put("BoundingBoxes", List.of(List.of(List.of(-90, -180), List.of(90, 180))));
            message.put("FilterMessageTypes", List.of(
                    "PositionReport",
                    "ExtendedClassBPositionReport",
                    "StandardClassBPositionReport",
                    "ShipStaticData",
                    "StaticDataReport"
            ));
            webSocket.sendText(objectMapper.writeValueAsString(message), true);
        } catch (Exception e) {
            log.error("发送 AISStream 订阅消息失败", e);
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);
        if (last) {
            String json = textBuffer.toString();
            textBuffer = new StringBuilder();
            try {
                processMessage(objectMapper.readTree(json));
            } catch (Exception e) {
                log.debug("解析 AIS 消息失败: {}", e.getMessage());
            }
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.info("AISStream 连接关闭: {} {}", statusCode, reason);
        closeSocket();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.warn("AISStream 连接异常: {}", error.getMessage());
        closeSocket();
    }

    private void closeSocket() {
        boolean wasActive = connected || webSocket != null;
        connected = false;
        WebSocket ws = webSocket;
        webSocket = null;
        if (ws != null) {
            try {
                ws.abort();
            } catch (Exception ignored) {
            }
        }
        if (wasActive) {
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        int delaySec = Math.min(60, 5 * (1 << Math.min(reconnectAttempt, 4)));
        reconnectAttempt++;
        scheduler.schedule(() -> {
            reconnectScheduled.set(false);
            connect();
        }, delaySec, TimeUnit.SECONDS);
        log.info("{} 秒后重连 AISStream", delaySec);
    }

    private void processMessage(JsonNode root) {
        JsonNode error = root.get("error");
        if (error != null && error.isTextual()) {
            log.warn("AISStream 服务端错误: {}", error.asText());
            return;
        }
        lastMessageAt = System.currentTimeMillis();
        String type = root.path("MessageType").asText();
        JsonNode meta = root.path("MetaData");
        String metaMmsi = meta.path("MMSI").asText();
        String metaName = meta.path("ShipName").asText();

        switch (type) {
            case "PositionReport", "ExtendedClassBPositionReport", "StandardClassBPositionReport" -> {
                JsonNode body = root.path("Message").path(type);
                String mmsi = metaMmsi.isBlank() ? body.path("UserID").asText() : metaMmsi;
                if (mmsi.isBlank()) {
                    return;
                }
                double lat = body.path("Latitude").asDouble();
                double lon = body.path("Longitude").asDouble();
                if (lat == 0 && lon == 0) {
                    return;
                }
                String name = metaName.isBlank() ? body.path("Name").asText() : metaName;
                updatePosition(mmsi, name, lat, lon,
                        body.path("Sog").asDouble(),
                        body.path("Cog").asDouble(),
                        body.path("TrueHeading").asDouble(),
                        body.path("NavigationalStatus").asInt(-1));
            }
            case "ShipStaticData" -> updateStatic(root.path("Message").path("ShipStaticData"), metaMmsi);
            case "StaticDataReport" ->
                    updateStatic(root.path("Message").path("StaticDataReport").path("ShipStaticData"), metaMmsi);
            default -> {
                if (!metaMmsi.isBlank() && !metaName.isBlank()) {
                    if (ships.size() < MAX_REGISTRY_SHIPS || ships.containsKey(metaMmsi)) {
                        ships.computeIfAbsent(metaMmsi, ShipData::new);
                        indexName(metaMmsi, metaName);
                    }
                }
            }
        }
    }

    private void updatePosition(String mmsi, String name, double lat, double lon,
                                double sog, double cog, double heading, int navStatus) {
        if (ships.size() >= MAX_REGISTRY_SHIPS && !ships.containsKey(mmsi)) {
            return;
        }
        long now = System.currentTimeMillis();
        ShipData ship = ships.computeIfAbsent(mmsi, ShipData::new);
        if (!name.isBlank()) {
            ship.name = name;
            indexName(mmsi, name);
        }
        ship.lat = lat;
        ship.lon = lon;
        ship.sog = sog;
        ship.cog = cog;
        ship.heading = heading;
        ship.navStatus = navStatus;
        ship.lastMsgAt = now;
        TrackData track = tracks.get(mmsi);
        if (track != null) {
            addTrackPoint(track, lat, lon, now, sog, cog);
        }
    }

    private void addTrackPoint(TrackData track, double lat, double lon, long now, double sog, double cog) {
        boolean shouldAdd = track.lastTrackAt == 0
                || now - track.lastTrackAt >= TRACK_POINT_INTERVAL_MS
                || Math.abs(lat - track.lastTrackLat) >= TRACK_POINT_DISTANCE_DEG
                || Math.abs(lon - track.lastTrackLon) >= TRACK_POINT_DISTANCE_DEG;
        if (!shouldAdd) {
            return;
        }
        track.lastTrackLat = lat;
        track.lastTrackLon = lon;
        track.lastTrackAt = now;
        synchronized (track.track) {
            track.track.addLast(new TrackPoint(lat, lon, now, sog, cog));
            while (track.track.size() > MAX_TRACK_POINTS) {
                track.track.removeFirst();
            }
        }
    }

    private void updateStatic(JsonNode body, String fallbackMmsi) {
        if (body == null || body.isMissingNode()) {
            return;
        }
        String mmsi = body.path("UserID").asText(fallbackMmsi);
        if (mmsi.isBlank()) {
            return;
        }
        if (ships.size() >= MAX_REGISTRY_SHIPS && !ships.containsKey(mmsi)) {
            return;
        }
        ShipData ship = ships.computeIfAbsent(mmsi, ShipData::new);
        String name = body.path("Name").asText();
        if (!name.isBlank()) {
            ship.name = name;
            indexName(mmsi, name);
        }
        String imo = body.path("ImoNumber").asText();
        if (!imo.isBlank()) {
            ship.imo = imo;
        }
        String callsign = body.path("CallSign").asText();
        if (!callsign.isBlank()) {
            ship.callsign = callsign;
        }
        int typeCode = body.path("Type").asInt(-1);
        if (typeCode >= 0) {
            ship.shipType = SHIP_TYPE_TEXT.getOrDefault(typeCode, "类型 " + typeCode);
        }
        JsonNode dimension = body.path("Dimension");
        if (!dimension.isMissingNode()) {
            double a = dimension.path("A").asDouble();
            double b = dimension.path("B").asDouble();
            double c = dimension.path("C").asDouble();
            double d = dimension.path("D").asDouble();
            if (a + b > 0) {
                ship.length = a + b;
            }
            if (c + d > 0) {
                ship.breadth = c + d;
            }
        }
        double draught = body.path("MaximumStaticDraught").asDouble();
        if (draught > 0) {
            ship.draught = draught;
        }
        String destination = body.path("Destination").asText();
        if (!destination.isBlank()) {
            ship.destination = destination;
        }
        String eta = formatEta(body.path("Eta"));
        if (eta != null) {
            ship.eta = eta;
        }
        ship.lastMsgAt = System.currentTimeMillis();
    }

    private String formatEta(JsonNode eta) {
        if (eta == null || eta.isMissingNode()) {
            return null;
        }
        int month = eta.path("Month").asInt(eta.path("UtcMonth").asInt(0));
        int day = eta.path("Day").asInt(eta.path("UtcDay").asInt(0));
        int hour = eta.path("Hour").asInt(eta.path("UtcHour").asInt(0));
        int minute = eta.path("Minute").asInt(eta.path("UtcMinute").asInt(0));
        if (month == 0 && day == 0 && hour == 0 && minute == 0) {
            return null;
        }
        return String.format(Locale.ROOT, "%02d-%02d %02d:%02d", month, day, hour, minute);
    }

    private void indexName(String mmsi, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        nameIndex.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(mmsi);
    }

    @Override
    public List<ShipLocateSearchVO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim();
        LinkedHashSet<String> mmsiSet = new LinkedHashSet<>();
        if (kw.matches("\\d{9}")) {
            if (ships.containsKey(kw)) {
                mmsiSet.add(kw);
            }
        } else if (kw.matches("\\d{7}")) {
            for (ShipData ship : ships.values()) {
                if (kw.equals(ship.imo)) {
                    mmsiSet.add(ship.mmsi);
                    break;
                }
            }
        } else {
            String key = kw.toLowerCase(Locale.ROOT);
            Set<String> exact = nameIndex.get(key);
            if (exact != null) {
                mmsiSet.addAll(exact);
            } else {
                for (Map.Entry<String, Set<String>> entry : nameIndex.entrySet()) {
                    if (entry.getKey().contains(key)) {
                        mmsiSet.addAll(entry.getValue());
                    }
                }
            }
        }
        return mmsiSet.stream()
                .map(ships::get)
                .filter(Objects::nonNull)
                .map(this::toSearchVO)
                .sorted(Comparator.comparing(vo -> vo.getName() == null ? "" : vo.getName()))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
    }

    private ShipLocateSearchVO toSearchVO(ShipData ship) {
        ShipLocateSearchVO vo = new ShipLocateSearchVO();
        vo.setMmsi(ship.mmsi);
        vo.setName(ship.name);
        vo.setImo(ship.imo);
        vo.setCallsign(ship.callsign);
        vo.setShipType(ship.shipType);
        vo.setLat(ship.lastMsgAt == 0 ? null : ship.lat);
        vo.setLon(ship.lastMsgAt == 0 ? null : ship.lon);
        vo.setLastUpdateAt(ship.lastMsgAt == 0 ? null : ship.lastMsgAt);
        return vo;
    }

    @Override
    public ShipLocateVO locate(String mmsi) {
        if (mmsi == null || !mmsi.matches("\\d{9}")) {
            return null;
        }
        ShipData ship = ships.get(mmsi);
        if (ship == null) {
            return null;
        }
        activateTrack(mmsi);
        ShipLocateVO vo = new ShipLocateVO();
        ShipLocateVO.ShipInfo info = new ShipLocateVO.ShipInfo();
        info.setMmsi(ship.mmsi);
        info.setName(ship.name);
        info.setImo(ship.imo);
        info.setCallsign(ship.callsign);
        info.setShipType(ship.shipType);
        info.setLength(ship.length > 0 ? ship.length : null);
        info.setBreadth(ship.breadth > 0 ? ship.breadth : null);
        info.setDraught(ship.draught > 0 ? ship.draught : null);
        info.setEta(ship.eta);
        info.setDestination(ship.destination);
        vo.setShip(info);

        if (ship.lastMsgAt > 0) {
            ShipLocateVO.LatestPosition latest = new ShipLocateVO.LatestPosition();
            latest.setLat(ship.lat);
            latest.setLon(ship.lon);
            latest.setSog(ship.sog);
            latest.setCog(ship.cog);
            latest.setHeading(ship.heading);
            latest.setNavStatus(ship.navStatus);
            latest.setTime(ship.lastMsgAt);
            vo.setLatest(latest);
        }

        List<ShipLocateVO.TrackPoint> track = List.of();
        TrackData trackData = tracks.get(mmsi);
        if (trackData != null) {
            synchronized (trackData.track) {
                track = trackData.track.stream()
                        .map(p -> new ShipLocateVO.TrackPoint(p.lat(), p.lon(), p.time(), p.sog(), p.cog()))
                        .toList();
            }
        }
        vo.setTrack(track);
        vo.setUpdateAt(System.currentTimeMillis());
        return vo;
    }

    /**
     * 用户查询过的船才开启轨迹缓存，避免为全球所有船都保存轨迹占用内存。
     */
    private void activateTrack(String mmsi) {
        if (tracks.containsKey(mmsi)) {
            return;
        }
        if (tracks.size() >= MAX_ACTIVE_TRACKS) {
            String oldestMmsi = null;
            long min = Long.MAX_VALUE;
            for (Map.Entry<String, TrackData> entry : tracks.entrySet()) {
                if (entry.getValue().lastTrackAt < min) {
                    min = entry.getValue().lastTrackAt;
                    oldestMmsi = entry.getKey();
                }
            }
            if (oldestMmsi != null) {
                tracks.remove(oldestMmsi);
            }
        }
        tracks.put(mmsi, new TrackData());
    }

    @Override
    public ShipLocateStatusVO status() {
        ShipLocateStatusVO vo = new ShipLocateStatusVO();
        vo.setConnected(connected);
        vo.setLastMessageAt(lastMessageAt);
        vo.setTrackedShips(ships.size());
        vo.setNameIndexSize(nameIndex.size());
        return vo;
    }

    @PreDestroy
    public void shutdown() {
        connected = false;
        if (webSocket != null) {
            try {
                webSocket.abort();
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
        scheduler.shutdownNow();
    }

    private record TrackPoint(double lat, double lon, long time, double sog, double cog) {
    }

    private static final class ShipData {
        final String mmsi;
        volatile String name = "";
        volatile String imo = "";
        volatile String callsign = "";
        volatile String shipType = "";
        volatile double length;
        volatile double breadth;
        volatile double draught;
        volatile String destination = "";
        volatile String eta = "";
        volatile double lat;
        volatile double lon;
        volatile double sog;
        volatile double cog;
        volatile double heading;
        volatile int navStatus = -1;
        volatile long lastMsgAt;

        ShipData(String mmsi) {
            this.mmsi = mmsi;
        }
    }

    private static final class TrackData {
        final Deque<TrackPoint> track = new ArrayDeque<>();
        volatile long lastTrackAt;
        volatile double lastTrackLat;
        volatile double lastTrackLon;
    }
}
