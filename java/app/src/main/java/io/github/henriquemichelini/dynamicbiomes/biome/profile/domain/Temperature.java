package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

public record Temperature(double normalized) {
    public Temperature {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException("Temperature must be within [0.0, 1.0]");
        }
    }
}
