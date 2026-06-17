package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public record WheatGrowthSeasonalFactor(double factor) {
    public WheatGrowthSeasonalFactor {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException(
                "Wheat growth seasonal factor must be a finite non-negative number"
            );
        }
    }
}
