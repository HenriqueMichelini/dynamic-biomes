package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

@FunctionalInterface
public interface TreeGrowthPolicyProvider {
    TreeGrowthPolicy policyFor(BiomeId biomeId);
}
