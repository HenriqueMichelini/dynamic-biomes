package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OreOriginWriteHistogramTest {
    @Test
    void p95UsesTheNearestRankBucketAcrossSuccessfulAndFailedAttempts() {
        OreOriginWriteHistogram histogram = new OreOriginWriteHistogram();
        for (int index = 0; index < 19; index++) {
            histogram.record(2_000_001L, true);
        }
        histogram.record(11_000_000_000L, false);

        assertEquals(Optional.of(Duration.ofMillis(3)), histogram.p95UpperBound());

        histogram.record(12_000_000_000L, false);
        assertEquals(Optional.of(Duration.ofSeconds(12)), histogram.p95UpperBound());
    }
}
