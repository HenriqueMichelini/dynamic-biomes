package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

public record CropYieldSeasonalFactor(double factor) {
    public CropYieldSeasonalFactor {
        if (!Double.isFinite(factor) || factor <= 0.0) {
            throw new IllegalArgumentException(
                "Crop yield seasonal factor must be a finite positive number"
            );
        }
    }
}
