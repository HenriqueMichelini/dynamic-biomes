package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

public record CropYieldEnvironmentalFactor(double factor) {
    public CropYieldEnvironmentalFactor {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException(
                "Crop yield environmental factor must be a finite non-negative number"
            );
        }
    }
}
