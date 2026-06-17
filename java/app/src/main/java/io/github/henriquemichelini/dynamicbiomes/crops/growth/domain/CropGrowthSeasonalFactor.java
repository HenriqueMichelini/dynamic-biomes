package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public record CropGrowthSeasonalFactor(double factor) {
    public CropGrowthSeasonalFactor {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException(
                "Crop growth seasonal factor must be a finite non-negative number"
            );
        }
    }
}
