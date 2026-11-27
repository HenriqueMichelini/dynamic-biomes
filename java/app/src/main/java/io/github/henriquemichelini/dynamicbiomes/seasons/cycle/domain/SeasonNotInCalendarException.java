package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class SeasonNotInCalendarException extends IllegalArgumentException {
    public SeasonNotInCalendarException(SeasonId season) {
        super("Season is not in the configured calendar: " + (season == null ? null : season.value()));
    }
}
