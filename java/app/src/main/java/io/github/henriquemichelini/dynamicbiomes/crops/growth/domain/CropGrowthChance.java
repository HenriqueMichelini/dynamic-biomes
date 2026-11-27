package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

public record CropGrowthChance(double value) {
    public CropGrowthChance {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new InvalidCropGrowthPolicyException(
                "Crop growth chance must be within [0.0, 1.0]"
            );
        }
    }

    public CropGrowthChance multipliedBy(double factor) {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException("Crop growth chance factor must be finite and not negative");
        }
        return new CropGrowthChance(Math.min(1.0, value * factor));
    }
}
