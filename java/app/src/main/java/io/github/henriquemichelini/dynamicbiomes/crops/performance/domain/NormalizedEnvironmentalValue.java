package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

public record NormalizedEnvironmentalValue(double normalized) {
    public NormalizedEnvironmentalValue {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException("Environmental value must be within [0.0, 1.0]");
        }
    }
}
