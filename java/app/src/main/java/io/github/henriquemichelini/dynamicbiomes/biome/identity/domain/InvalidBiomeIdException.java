package io.github.henriquemichelini.dynamicbiomes.biome.identity.domain;

public final class InvalidBiomeIdException extends IllegalArgumentException {
    public InvalidBiomeIdException(String message) {
        super(message);
    }
}
