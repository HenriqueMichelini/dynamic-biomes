package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

public enum OreOriginType {
    NATURAL,
    PLAYER_PLACED;

    public boolean isEligibleForBiomeBasedMultiplier() {
        return this == NATURAL;
    }
}
