package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import lombok.NonNull;

public record ClimateProfile(
    @NonNull Humidity humidity,
    @NonNull Temperature temperature
) {
    public ClimateProfile {}
}
