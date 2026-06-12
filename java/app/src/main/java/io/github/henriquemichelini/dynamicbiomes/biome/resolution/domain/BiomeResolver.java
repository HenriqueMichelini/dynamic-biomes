package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public interface BiomeResolver {
    BiomeContext resolve(BlockPosition position);
}
