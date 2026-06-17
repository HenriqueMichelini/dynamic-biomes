package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public record CropGrowthChance(double value) {
    public CropGrowthChance {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                "Crop growth chance must be within [0.0, 1.0]"
            );
        }
    }
}
