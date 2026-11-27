package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonNotInCalendarException;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class InvalidPersistedSeasonException extends IllegalStateException {
    public InvalidPersistedSeasonException(SeasonId season, SeasonNotInCalendarException cause) {
        super("Persisted current season is not in configured calendar: " + season.value(), cause);
    }
}
