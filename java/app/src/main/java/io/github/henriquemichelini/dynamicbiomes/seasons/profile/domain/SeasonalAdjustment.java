package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

public record SeasonalAdjustment(double normalized) {
    public SeasonalAdjustment {
        if (!Double.isFinite(normalized) || normalized < -1.0 || normalized > 1.0) {
            throw new IllegalArgumentException(
                "Seasonal adjustment must be within [-1.0, 1.0]"
            );
        }
    }
}
