package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

public record OreDropMultiplierRange(double minimum, double maximum) {
    public OreDropMultiplierRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Ore drop multiplier bounds must be finite");
        }
        if (minimum < 0.0 || maximum < 0.0) {
            throw new IllegalArgumentException("Ore drop multiplier bounds must not be negative");
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum ore drop multiplier must not exceed maximum");
        }
    }
}
