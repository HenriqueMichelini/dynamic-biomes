package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

public record SeasonCycleSettings(
    boolean enabled,
    long initialDelayTicks,
    long intervalTicks
) {
    public SeasonCycleSettings {
        if (initialDelayTicks < 0) {
            throw new IllegalArgumentException(
                "initial-delay-ticks must be non-negative"
            );
        }
        if (intervalTicks <= 0) {
            throw new IllegalArgumentException(
                "interval-ticks must be positive"
            );
        }
    }
}
