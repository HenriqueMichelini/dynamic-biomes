package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import lombok.NonNull;

public record BiomeContext(
    @NonNull BiomeId biomeId,
    @NonNull BiomeProfile profile
) {
    public BiomeContext {
        if (!biomeId.equals(profile.biomeId())) {
            throw new IllegalArgumentException(
                "Resolved biome identity must match biome profile identity"
            );
        }
    }
}
