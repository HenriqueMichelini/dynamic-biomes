package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class SeasonAdvancementService {
    private final SeasonCalendar calendar;
    private final SeasonStateRepository repository;

    public SeasonAdvancementService(
        SeasonCalendar calendar,
        SeasonStateRepository repository
    ) {
        this.calendar = calendar;
        this.repository = repository;
    }

    public SeasonId advance() {
        SeasonId currentSeason = repository.findCurrentSeason()
            .orElseThrow(() -> new IllegalStateException("Current season is not initialized"));
        SeasonId nextSeason = calendar.nextAfter(currentSeason);
        repository.saveCurrentSeason(nextSeason);
        return nextSeason;
    }
}
