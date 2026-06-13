package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import lombok.NonNull;

public record SeasonProfile(
    @NonNull SeasonId seasonId,
    @NonNull SeasonClimateAdjustment climateAdjustment
) {
    public SeasonProfile {
    }
}
