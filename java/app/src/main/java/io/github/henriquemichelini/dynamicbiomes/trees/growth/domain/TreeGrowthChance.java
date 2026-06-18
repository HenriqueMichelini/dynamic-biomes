package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

public record TreeGrowthChance(double value) {
    public TreeGrowthChance {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                "Tree growth chance must be within [0.0, 1.0]"
            );
        }
    }
}
