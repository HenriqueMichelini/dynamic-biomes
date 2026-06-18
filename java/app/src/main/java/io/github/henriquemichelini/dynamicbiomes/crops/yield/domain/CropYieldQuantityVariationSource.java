package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

@FunctionalInterface
public interface CropYieldQuantityVariationSource {
    double nextUnitValue();
}
