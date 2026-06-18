package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import lombok.NonNull;

public final class CropYieldMultiplierCalculator {

    private final CropYieldMultiplierVariationSource variationSource;

    public CropYieldMultiplierCalculator(
        @NonNull CropYieldMultiplierVariationSource variationSource
    ) {
        this.variationSource = variationSource;
    }

    public double calculate(@NonNull CropYieldMultiplierRange range) {
        if (range.minimum() == range.maximum()) {
            return range.minimum();
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException(
                "Crop yield multiplier variation must be within [0.0, 1.0]"
            );
        }
        if (variation == 0.0) {
            return range.minimum();
        }
        if (variation == 1.0) {
            return range.maximum();
        }

        double multiplier =
            range.minimum() + (range.maximum() - range.minimum()) * variation;
        return Math.clamp(multiplier, range.minimum(), range.maximum());
    }
}
