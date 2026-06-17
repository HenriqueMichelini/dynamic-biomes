package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

@FunctionalInterface
public interface CropGrowthChanceVariationSource {
    double nextUnitValue();
}
