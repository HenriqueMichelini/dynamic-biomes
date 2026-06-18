package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;

public final class CropYieldCropRule {
    private final CropYieldMultiplierRange multiplierRange;
    private final Map<SeasonId, CropYieldSeasonalFactor> seasonalFactors;

    public CropYieldCropRule(
        CropYieldMultiplierRange multiplierRange,
        Map<SeasonId, CropYieldSeasonalFactor> seasonalFactors
    ) {
        if (multiplierRange == null) {
            throw new IllegalArgumentException(
                "Crop yield rule multiplier range must not be null"
            );
        }
        this.multiplierRange = multiplierRange;
        this.seasonalFactors = seasonalFactors == null
            ? Map.of()
            : Map.copyOf(seasonalFactors);
    }

    public CropYieldMultiplierRange multiplierRange() {
        return multiplierRange;
    }

    public double seasonalFactorFor(SeasonId seasonId) {
        CropYieldSeasonalFactor seasonalFactor = seasonalFactors.get(seasonId);
        return seasonalFactor == null ? 1.0 : seasonalFactor.factor();
    }
}
