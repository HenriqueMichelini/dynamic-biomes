package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface OreDropPolicyProvider {
    OreDropPolicy policyFor(BiomeId biomeId);
}
