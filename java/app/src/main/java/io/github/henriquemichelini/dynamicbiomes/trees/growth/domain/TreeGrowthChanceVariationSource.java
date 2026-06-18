package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

@FunctionalInterface
public interface TreeGrowthChanceVariationSource {
    double nextUnitValue();
}
