package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface WheatGrowthChancePolicyProvider {
    WheatGrowthChancePolicy policyFor(BiomeId biomeId);
}
