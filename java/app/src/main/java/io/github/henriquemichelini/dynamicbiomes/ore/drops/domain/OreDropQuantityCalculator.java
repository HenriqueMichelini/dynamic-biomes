package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import java.util.Objects;

public final class OreDropQuantityCalculator {
    private final OreDropQuantityVariationSource variationSource;

    public OreDropQuantityCalculator(OreDropQuantityVariationSource variationSource) {
        this.variationSource = Objects.requireNonNull(variationSource, "variationSource");
    }

    public int calculate(int vanillaDropQuantity, double multiplier) {
        if (vanillaDropQuantity < 0) {
            throw new IllegalArgumentException("Vanilla ore drop quantity must not be negative");
        }
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("Ore drop multiplier must be finite and not negative");
        }

        double multipliedQuantity = vanillaDropQuantity * multiplier;
        if (!Double.isFinite(multipliedQuantity) || multipliedQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Multiplied ore drop quantity exceeds supported quantity");
        }

        int guaranteedQuantity = (int) Math.floor(multipliedQuantity);
        double fractionalRemainder = multipliedQuantity - guaranteedQuantity;
        if (fractionalRemainder == 0.0) {
            return guaranteedQuantity;
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException("Ore drop quantity variation must be within [0.0, 1.0]");
        }

        return variation < fractionalRemainder
            ? guaranteedQuantity + 1
            : guaranteedQuantity;
    }
}
