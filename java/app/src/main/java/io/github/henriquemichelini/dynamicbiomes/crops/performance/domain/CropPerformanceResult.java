package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import java.util.OptionalDouble;
import lombok.NonNull;

public record CropPerformanceResult(
    @NonNull OptionalDouble overallScore,
    double growthSpeedFactor,
    double growthChanceFactor,
    double harvestQuantityFactor
) {
    public CropPerformanceResult {
        validateOptionalScore(overallScore);
        validateFactor(growthSpeedFactor, "growthSpeedFactor");
        validateFactor(growthChanceFactor, "growthChanceFactor");
        validateFactor(harvestQuantityFactor, "harvestQuantityFactor");
    }

    private static void validateOptionalScore(OptionalDouble score) {
        if (score.isPresent()) {
            double value = score.getAsDouble();
            if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(
                    "Overall score must be empty or within [0.0, 1.0]"
                );
            }
        }
    }

    private static void validateFactor(double factor, String name) {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException(
                name + " must be a finite non-negative value"
            );
        }
    }
}
