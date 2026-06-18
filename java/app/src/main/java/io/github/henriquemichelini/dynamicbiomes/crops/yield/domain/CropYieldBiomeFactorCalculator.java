package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import lombok.NonNull;

public final class CropYieldBiomeFactorCalculator {

    public CropYieldEnvironmentalFactor calculate(@NonNull Fertility fertility) {
        return new CropYieldEnvironmentalFactor(
            1.0 + ((fertility.normalized() - 0.5) * 0.40)
        );
    }
}
