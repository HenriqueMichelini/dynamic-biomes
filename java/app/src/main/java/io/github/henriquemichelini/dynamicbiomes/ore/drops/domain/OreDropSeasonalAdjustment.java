package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

public record OreDropSeasonalAdjustment(double multiplierFactor) {
    public OreDropSeasonalAdjustment {
        if (!Double.isFinite(multiplierFactor) || multiplierFactor <= 0.0) {
            throw new IllegalArgumentException(
                "Seasonal multiplier factor must be a finite positive number"
            );
        }
    }
}
