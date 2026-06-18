package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface CropGrowthPolicyProvider {
    CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind);
}
