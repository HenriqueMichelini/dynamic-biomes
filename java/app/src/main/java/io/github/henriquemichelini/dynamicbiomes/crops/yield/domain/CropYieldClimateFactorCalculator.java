package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import lombok.NonNull;

public final class CropYieldClimateFactorCalculator {

    public CropYieldEnvironmentalFactor calculate(
        @NonNull SeasonClimateAdjustment climateAdjustment
    ) {
        double averageAdjustment =
            (climateAdjustment.temperature().normalized() +
                climateAdjustment.humidity().normalized()) / 2.0;

        return new CropYieldEnvironmentalFactor(
            1.0 + (averageAdjustment * 0.15)
        );
    }
}
