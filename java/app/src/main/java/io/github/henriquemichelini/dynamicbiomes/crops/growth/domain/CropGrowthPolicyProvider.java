package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;

public interface CropGrowthPolicyProvider {
    CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind);
}
