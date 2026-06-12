package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

public record MineralRichness(double normalized) {
    public MineralRichness {
        if (!Double.isFinite(normalized) || normalized < 0.0 || normalized > 1.0) {
            throw new IllegalArgumentException(
                "Mineral richness must be within [0.0, 1.0]"
            );
        }
    }
}
