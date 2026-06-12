package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

public record Humidity(double normalized) {
    public Humidity {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException("Humidity must be within [0.0, 1.0]");
        }
    }
}
