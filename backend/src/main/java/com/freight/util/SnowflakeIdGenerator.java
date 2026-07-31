package com.freight.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器（Twitter Snowflake）
 *
 * 64-bit 结构：
 * | 1 bit 符号 | 41 bit 时间戳 | 5 bit 数据中心 | 5 bit 机器 | 12 bit 序列号 |
 *
 * - 时间戳可用约 69 年（2024-01-01 起）
 * - 支持 32 个数据中心 × 32 台机器 = 1024 个节点
 * - 每节点每毫秒可生成 4096 个 ID
 */
@Component
public class SnowflakeIdGenerator {

    /** 起始纪元：2024-01-01 00:00:00 UTC */
    private static final long EPOCH = 1704067200000L;

    private static final long DATACENTER_ID_BITS = 5L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long datacenterId;
    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${snowflake.datacenter-id:1}") long datacenterId,
            @Value("${snowflake.worker-id:1}") long workerId) {
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("datacenterId 必须在 0~" + MAX_DATACENTER_ID + " 之间");
        }
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId 必须在 0~" + MAX_WORKER_ID + " 之间");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 生成下一个唯一 ID（线程安全）
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 回拨 5ms 以内，等待追上
                try {
                    wait(offset << 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("雪花算法等待时钟同步被中断", e);
                }
                timestamp = System.currentTimeMillis();
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException("时钟回拨超过容忍范围: " + (lastTimestamp - timestamp) + "ms");
                }
            } else {
                throw new RuntimeException("时钟回拨超过容忍范围: " + offset + "ms");
            }
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成订单编号：ORD + 雪花ID（共 22 位，VARCHAR(30) 安全）
     */
    public String nextOrderNo() {
        return "ORD" + nextId();
    }

    private long waitNextMillis(long lastTs) {
        long ts = System.currentTimeMillis();
        while (ts <= lastTs) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
