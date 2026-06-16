package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public record WheatGrowthChance(double value) {
    public WheatGrowthChance {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                "Wheat growth chance must be within [0.0, 1.0]"
            );
        }
    }
}
