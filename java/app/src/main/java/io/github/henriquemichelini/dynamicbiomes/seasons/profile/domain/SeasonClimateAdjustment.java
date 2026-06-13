package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import lombok.NonNull;

public record SeasonClimateAdjustment(
    @NonNull SeasonalAdjustment temperature,
    @NonNull SeasonalAdjustment humidity
) {
    public SeasonClimateAdjustment {
    }
}
