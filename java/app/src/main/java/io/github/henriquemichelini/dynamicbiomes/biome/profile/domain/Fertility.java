package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

public record Fertility(double normalized) {
    public Fertility {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException("Fertility must be within [0.0, 1.0]");
        }
    }
}
