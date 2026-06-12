package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import lombok.NonNull;

public final class OreDropMultiplierCalculator {

    private final OreDropMultiplierVariationSource variationSource;

    public OreDropMultiplierCalculator(
        @NonNull OreDropMultiplierVariationSource variationSource
    ) {
        this.variationSource = variationSource;
    }

    public double calculate(@NonNull OreDropMultiplierRange range) {
        if (range.minimum() == range.maximum()) {
            return range.minimum();
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException(
                "Ore drop multiplier variation must be within [0.0, 1.0]"
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
