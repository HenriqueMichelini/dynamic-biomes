package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public interface BiomeResolver {
    /** @throws UnsupportedBiomeException if the resolved biome has no supported profile */
    BiomeContext resolve(BlockPosition position);
}
