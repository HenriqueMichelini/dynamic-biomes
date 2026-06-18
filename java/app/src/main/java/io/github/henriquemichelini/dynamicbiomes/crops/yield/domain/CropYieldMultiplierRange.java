package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

public record CropYieldMultiplierRange(double minimum, double maximum) {
    public CropYieldMultiplierRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Crop yield multiplier bounds must be finite");
        }
        if (minimum < 0.0 || maximum < 0.0) {
            throw new IllegalArgumentException("Crop yield multiplier bounds must not be negative");
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum crop yield multiplier must not exceed maximum");
        }
    }
}
