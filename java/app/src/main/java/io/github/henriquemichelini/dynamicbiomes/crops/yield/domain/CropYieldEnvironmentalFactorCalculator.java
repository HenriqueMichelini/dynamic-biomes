package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import lombok.NonNull;

public final class CropYieldEnvironmentalFactorCalculator {

    public CropYieldEnvironmentalFactor calculate(
        @NonNull CropYieldEnvironmentalFactor biomeYieldFactor,
        @NonNull CropYieldEnvironmentalFactor climateYieldFactor
    ) {
        return new CropYieldEnvironmentalFactor(
            biomeYieldFactor.factor() * climateYieldFactor.factor()
        );
    }
}
