package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import java.util.Objects;

public final class OreDropMultiplierCalculator {
    private final OreDropMultiplierVariationSource variationSource;

    public OreDropMultiplierCalculator(OreDropMultiplierVariationSource variationSource) {
        this.variationSource = Objects.requireNonNull(variationSource, "variationSource");
    }

    public double calculate(OreDropMultiplierRange range) {
        Objects.requireNonNull(range, "range");

        if (range.minimum() == range.maximum()) {
            return range.minimum();
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException("Ore drop multiplier variation must be within [0.0, 1.0]");
        }
        if (variation == 0.0) {
            return range.minimum();
        }
        if (variation == 1.0) {
            return range.maximum();
        }

        double multiplier = range.minimum() + (range.maximum() - range.minimum()) * variation;
        return Math.clamp(multiplier, range.minimum(), range.maximum());
    }
}
