package io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain;

public final class InvalidSeasonIdException extends IllegalArgumentException {
    public InvalidSeasonIdException(String message) {
        super(message);
    }
}
