package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import java.util.Objects;

public record SeasonCycleSettings(
    boolean enabled,
    long initialDelayTicks,
    long intervalTicks,
    SeasonCalendar calendar
) {
    public SeasonCycleSettings(boolean enabled, long initialDelayTicks, long intervalTicks) {
        this(enabled, initialDelayTicks, intervalTicks, SeasonCalendar.standard());
    }

    public SeasonCycleSettings {
        Objects.requireNonNull(calendar, "Season calendar is required");
        if (initialDelayTicks < 0) {
            throw new InvalidSeasonCycleSettingsException(
                "initial-delay-ticks must be non-negative"
            );
        }
        if (intervalTicks <= 0) {
            throw new InvalidSeasonCycleSettingsException(
                "interval-ticks must be positive"
            );
        }
    }
}
