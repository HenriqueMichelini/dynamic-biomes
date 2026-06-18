package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

public record TreeGrowthSeasonalFactor(double factor) {
    public TreeGrowthSeasonalFactor {
        if (!Double.isFinite(factor) || factor < 0.0) {
            throw new IllegalArgumentException(
                "Tree growth seasonal factor must be a finite non-negative number"
            );
        }
    }
}
