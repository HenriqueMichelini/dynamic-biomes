package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

public record EcologicalPressure(double normalized) {
    public EcologicalPressure {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException(
                "Ecological pressure must be within [0.0, 1.0]"
            );
        }
    }
}
