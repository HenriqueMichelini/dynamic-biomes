package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState;
import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import java.util.Optional;
import lombok.NonNull;

public record BiomeContext(
    @NonNull BiomeId biomeId,
    @NonNull BiomeProfile profile,
    @NonNull Optional<EcologicalRegionState> ecologicalState
) {
    public BiomeContext(BiomeId biomeId, BiomeProfile profile) {
        this(biomeId, profile, Optional.empty());
    }

    public BiomeContext {
        if (!biomeId.equals(profile.biomeId())) {
            throw new IllegalArgumentException(
                "Resolved biome identity must match biome profile identity"
            );
        }
    }
}
