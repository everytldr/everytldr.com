package com.everytldr.common.domain.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.EPOCH_MS;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.MAX_BACKWARD_TOLERANCE_MS;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.MAX_SEQUENCE;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.MAX_WORKER_ID;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.TIMESTAMP_SHIFT;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.WORKER_BITS;
import static com.everytldr.common.domain.support.SnowflakeIdGenerator.WORKER_SHIFT;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {
  @Test
  void generatedIdsAreStrictlyIncreasing() {
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(Clock.systemUTC(), 0L);
    long previous = generator.generateId();

    for (int i = 0; i < 10_000; i++) {
      long current = generator.generateId();
      assertThat(current).isGreaterThan(previous);
      previous = current;
    }
  }

  @Test
  void rejectsWorkerIdOutOfRange() {
    Clock clock = Clock.systemUTC();
    assertThatThrownBy(() -> new SnowflakeIdGenerator(clock, -1L))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SnowflakeIdGenerator(clock, MAX_WORKER_ID + 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsBoundaryWorkerIds() {
    Clock clock = Clock.systemUTC();
    new SnowflakeIdGenerator(clock, 0L);
    new SnowflakeIdGenerator(clock, MAX_WORKER_ID);
  }

  @Test
  void encodesTimestampWorkerAndSequenceBits() {
    MutableClock clock = new MutableClock(EPOCH_MS + 12_345L);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(clock, 7L);

    long first = generator.generateId();
    long second = generator.generateId();

    long workerMask = (1L << WORKER_BITS) - 1L;
    assertThat(first >>> TIMESTAMP_SHIFT).isEqualTo(12_345L);
    assertThat((first >>> WORKER_SHIFT) & workerMask).isEqualTo(7L);
    assertThat(first & MAX_SEQUENCE).isEqualTo(0L);
    assertThat(second & MAX_SEQUENCE).isEqualTo(1L);
  }

  @Test
  void fillsSequenceWithinSameMillisecond() {
    MutableClock clock = new MutableClock(EPOCH_MS + 1_000L);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(clock, 0L);

    long firstTimestamp = generator.generateId() >>> TIMESTAMP_SHIFT;
    for (long i = 1L; i <= MAX_SEQUENCE; i++) {
      long id = generator.generateId();
      assertThat(id >>> TIMESTAMP_SHIFT).isEqualTo(firstTimestamp);
      assertThat(id & MAX_SEQUENCE).isEqualTo(i);
    }
  }

  @Test
  void throwsWhenClockMovesBackwardsBeyondTolerance() {
    MutableClock clock = new MutableClock(EPOCH_MS + MAX_BACKWARD_TOLERANCE_MS);
    SnowflakeIdGenerator generator = new SnowflakeIdGenerator(clock, 0L);
    generator.generateId();

    clock.set(EPOCH_MS);

    assertThatThrownBy(generator::generateId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Clock moved backwards");
  }

  private static final class MutableClock extends Clock {

    private long currentMs;

    MutableClock(long initial) {
      this.currentMs = initial;
    }

    void set(long ms) {
      this.currentMs = ms;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(currentMs);
    }

    @Override
    public long millis() {
      return currentMs;
    }
  }
}
