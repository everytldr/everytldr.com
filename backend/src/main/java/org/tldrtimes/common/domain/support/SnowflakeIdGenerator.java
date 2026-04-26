package org.tldrtimes.common.domain.support;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    static final long EPOCH_MS = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
    static final int WORKER_BITS = 10;
    static final int SEQUENCE_BITS = 12;
    static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1L;
    static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;
    static final int WORKER_SHIFT = SEQUENCE_BITS;
    static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    static final long MAX_BACKWARD_TOLERANCE_MS = 1000L;

    private final Clock clock;
    private final long workerId;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(
            Clock clock,
            @Value("${tldrtimes.snowflake.worker-id:0}") long workerId) {
        if (workerId < 0L || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "worker-id must be in [0, %d], got %d"
                            .formatted(MAX_WORKER_ID, workerId));
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.workerId = workerId;
    }

    public synchronized long generateId() {
        long now = clock.millis();

        if (now < lastTimestamp) {
            long backwardMs = lastTimestamp - now;
            if (backwardMs >= MAX_BACKWARD_TOLERANCE_MS) {
                throw new IllegalStateException(
                        "Clock moved backwards by %d ms (tolerance %d ms)"
                                .formatted(backwardMs, MAX_BACKWARD_TOLERANCE_MS));
            }
            spinUntil(lastTimestamp);
            now = clock.millis();
        }

        if (now == lastTimestamp) {
            if (sequence < MAX_SEQUENCE) {
                sequence++;
            } else {
                spinUntil(lastTimestamp + 1L);
                now = clock.millis();
                sequence = 0L;
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = now;
        long delta = now - EPOCH_MS;
        return (delta << TIMESTAMP_SHIFT) | (workerId << WORKER_SHIFT) | sequence;
    }

    private void spinUntil(long targetMs) {
        while (clock.millis() < targetMs) {
            Thread.onSpinWait();
        }
    }
}
