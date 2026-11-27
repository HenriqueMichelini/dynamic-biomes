package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

public record OreDropMultiplierRange(double minimum, double maximum) {
    public OreDropMultiplierRange {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new InvalidOreDropPolicyException("Ore drop multiplier bounds must be finite");
        }
        if (minimum < 0.0 || maximum < 0.0) {
            throw new InvalidOreDropPolicyException("Ore drop multiplier bounds must not be negative");
        }
        if (minimum > maximum) {
            throw new InvalidOreDropPolicyException("Minimum ore drop multiplier must not exceed maximum");
        }
    }
}
