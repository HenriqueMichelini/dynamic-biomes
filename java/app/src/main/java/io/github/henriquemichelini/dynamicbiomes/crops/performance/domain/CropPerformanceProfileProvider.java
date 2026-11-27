package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;

public interface CropPerformanceProfileProvider {
    /** @throws UnsupportedCropPerformanceProfileException if the crop has no configured preferences */
    CropPerformanceProfile profileFor(CropKind cropKind);
}
