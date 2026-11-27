package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class MissingSeasonProfileException extends IllegalArgumentException {
    public MissingSeasonProfileException(SeasonId season) {
        super("Missing season profile: " + season.value());
    }
}
