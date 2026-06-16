package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

@FunctionalInterface
public interface WheatGrowthChanceVariationSource {
    double nextUnitValue();
}
