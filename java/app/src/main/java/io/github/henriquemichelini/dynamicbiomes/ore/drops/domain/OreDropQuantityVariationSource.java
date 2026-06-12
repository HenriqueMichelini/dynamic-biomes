package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

@FunctionalInterface
public interface OreDropQuantityVariationSource {
    double nextUnitValue();
}
