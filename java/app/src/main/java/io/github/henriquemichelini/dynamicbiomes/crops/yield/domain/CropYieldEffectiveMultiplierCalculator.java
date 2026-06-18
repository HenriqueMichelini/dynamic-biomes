package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import lombok.NonNull;

public final class CropYieldEffectiveMultiplierCalculator {

    public double calculate(
        double selectedBaseCropMultiplier,
        @NonNull CropYieldSeasonalFactor cropSeasonalFactor,
        @NonNull CropYieldEnvironmentalFactor environmentalFactor
    ) {
        if (!Double.isFinite(selectedBaseCropMultiplier)
            || selectedBaseCropMultiplier < 0.0) {
            throw new IllegalArgumentException(
                "Selected base crop multiplier must be a finite non-negative number"
            );
        }

        return selectedBaseCropMultiplier
            * cropSeasonalFactor.factor()
            * environmentalFactor.factor();
    }
}
