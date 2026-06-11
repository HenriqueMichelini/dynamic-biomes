package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

public record OreDropRequest(
    OreKind oreKind,
    OreDropMultiplierRange effectRange
) {}
