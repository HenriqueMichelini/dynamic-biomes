package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import lombok.NonNull;

public record OreOrigin(
    @NonNull BlockPosition position,
    @NonNull OreOriginType type
) {
    public OreOrigin {
    }

    public boolean isEligibleForBiomeBasedMultiplier() {
        return type == OreOriginType.NATURAL;
    }
}
