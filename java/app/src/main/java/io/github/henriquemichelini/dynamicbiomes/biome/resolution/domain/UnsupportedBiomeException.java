package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import java.util.Optional;

public final class UnsupportedBiomeException extends RuntimeException {
    private final BiomeId biomeId;

    public UnsupportedBiomeException(String message) {
        this(null, message);
    }

    public UnsupportedBiomeException(BiomeId biomeId, String message) {
        super(message);
        this.biomeId = biomeId;
    }

    public Optional<BiomeId> biomeId() {
        return Optional.ofNullable(biomeId);
    }
}
