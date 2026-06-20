package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;

public interface CropPerformanceProfileProvider {
    CropPerformanceProfile profileFor(CropKind cropKind);
}
