package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;

public interface CropGrowthPolicyProvider {
    /** @throws UnsupportedCropGrowthPolicyException if no growth rule applies to the biome and crop */
    CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind);
}
