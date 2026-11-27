package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface CropYieldPolicyProvider {
    /** @throws UnsupportedCropYieldPolicyException if no yield policy applies to the biome */
    CropYieldPolicy policyFor(BiomeId biomeId);
}
