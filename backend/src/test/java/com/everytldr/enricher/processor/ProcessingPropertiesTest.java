package com.everytldr.enricher.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ProcessingPropertiesTest {

  @Test
  void calculatesCappedExponentialRetryDelay() {
    ProcessingProperties properties =
        properties(Duration.ofMinutes(1), 2.0, Duration.ofMinutes(10));

    assertThat(properties.calculateRetryDelay(1)).isEqualTo(Duration.ofMinutes(1));
    assertThat(properties.calculateRetryDelay(2)).isEqualTo(Duration.ofMinutes(2));
    assertThat(properties.calculateRetryDelay(5)).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void rejectsInvalidRetryProperties() {
    assertThatThrownBy(
            () ->
                new ProcessingProperties.RetryProperties(
                    Duration.ofMinutes(1), 1.0, Duration.ofMinutes(10)))
        .hasMessage("retry.multiplier must be greater than 1.0");
    assertThatThrownBy(
            () ->
                new ProcessingProperties.RetryProperties(
                    Duration.ofMinutes(10), 2.0, Duration.ofMinutes(1)))
        .hasMessage("retry.maxInterval must not be shorter than retry.initialInterval");
  }

  private ProcessingProperties properties(
      Duration initialInterval, double multiplier, Duration maxInterval) {
    return new ProcessingProperties(
        false,
        10,
        Duration.ofSeconds(30),
        3,
        new ProcessingProperties.RetryProperties(initialInterval, multiplier, maxInterval),
        Duration.ofMinutes(15));
  }
}
