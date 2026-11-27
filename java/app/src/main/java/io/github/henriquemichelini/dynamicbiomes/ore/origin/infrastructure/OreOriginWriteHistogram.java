package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/** Cumulative write timings, accessed under the repository's lock. */
final class OreOriginWriteHistogram {
    private static final long[] BUCKET_UPPER_BOUNDS_NANOS = {
        10_000, 25_000, 50_000, 100_000, 250_000, 500_000, 750_000,
        1_000_000, 1_500_000, 2_000_000, 3_000_000, 4_000_000, 6_000_000, 8_000_000,
        12_000_000, 16_000_000, 24_000_000, 32_000_000, 48_000_000, 64_000_000,
        96_000_000, 128_000_000, 192_000_000, 256_000_000, 384_000_000,
        512_000_000, 768_000_000, 1_000_000_000, 2_000_000_000L, 4_000_000_000L, 10_000_000_000L
    };

    private final long[] buckets = new long[BUCKET_UPPER_BOUNDS_NANOS.length + 1];
    private long successes;
    private long failures;
    private long longestOverflowNanos;

    void record(long durationNanos, boolean successful) {
        int bucket = Arrays.binarySearch(BUCKET_UPPER_BOUNDS_NANOS, durationNanos);
        if (bucket < 0) {
            bucket = -bucket - 1;
        }
        buckets[bucket]++;
        if (bucket == BUCKET_UPPER_BOUNDS_NANOS.length) {
            longestOverflowNanos = Math.max(longestOverflowNanos, durationNanos);
        }
        if (successful) {
            successes++;
        } else {
            failures++;
        }
    }

    long successes() {
        return successes;
    }

    long failures() {
        return failures;
    }

    Optional<Duration> p95UpperBound() {
        long attempts = successes + failures;
        if (attempts == 0) {
            return Optional.empty();
        }

        // The nearest rank for p95 is ceil(0.95 * attempts).
        long rank = attempts - attempts / 20;
        for (int bucket = 0; bucket < BUCKET_UPPER_BOUNDS_NANOS.length; bucket++) {
            rank -= buckets[bucket];
            if (rank <= 0) {
                return Optional.of(Duration.ofNanos(BUCKET_UPPER_BOUNDS_NANOS[bucket]));
            }
        }
        return Optional.of(Duration.ofNanos(longestOverflowNanos));
    }
}
