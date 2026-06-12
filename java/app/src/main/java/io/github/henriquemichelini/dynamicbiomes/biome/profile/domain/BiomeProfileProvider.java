package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface BiomeProfileProvider {
    BiomeProfile profileFor(BiomeId biomeId);
}
