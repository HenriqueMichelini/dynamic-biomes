package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import lombok.NonNull;

public final class CropYieldQuantityCalculator {
    private final CropYieldQuantityVariationSource variationSource;

    public CropYieldQuantityCalculator(
        @NonNull CropYieldQuantityVariationSource variationSource
    ) {
        this.variationSource = variationSource;
    }

    public int calculate(int vanillaProduceQuantity, double multiplier) {
        if (vanillaProduceQuantity < 0) {
            throw new IllegalArgumentException("Vanilla crop produce quantity must not be negative");
        }
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("Crop yield multiplier must be finite and not negative");
        }

        double multipliedQuantity = vanillaProduceQuantity * multiplier;
        if (!Double.isFinite(multipliedQuantity) || multipliedQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Multiplied crop produce quantity exceeds supported quantity");
        }

        int guaranteedQuantity = (int) Math.floor(multipliedQuantity);
        double fractionalRemainder = multipliedQuantity - guaranteedQuantity;
        if (fractionalRemainder == 0.0) {
            return guaranteedQuantity;
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException(
                "Crop yield quantity variation must be within [0.0, 1.0]"
            );
        }

        return variation < fractionalRemainder
            ? guaranteedQuantity + 1
            : guaranteedQuantity;
    }
}
