package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface CropYieldPolicyProvider {
    CropYieldPolicy policyFor(BiomeId biomeId);
}
