package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import java.util.Objects;

public record OreDropEnvironmentContext(BiomeContext biomeContext, SeasonProfile seasonProfile) {
    public OreDropEnvironmentContext {
        Objects.requireNonNull(biomeContext, "Biome context must not be null");
        Objects.requireNonNull(seasonProfile, "Season profile must not be null");
    }
}
