package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import java.util.Objects;

public record BiomeContext(BiomeId biomeId, BiomeProfile profile) {
    public BiomeContext {
        Objects.requireNonNull(biomeId, "Resolved biome identity must not be null");
        Objects.requireNonNull(profile, "Resolved biome profile must not be null");
        if (!biomeId.equals(profile.biomeId())) {
            throw new IllegalArgumentException(
                "Resolved biome identity must match biome profile identity"
            );
        }
    }
}
