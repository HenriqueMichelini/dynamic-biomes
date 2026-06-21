package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import lombok.NonNull;

public record BiomeProfile(
    @NonNull BiomeId biomeId,
    @NonNull ClimateProfile climate,
    @NonNull Fertility fertility
) {
    public BiomeProfile {}
}
