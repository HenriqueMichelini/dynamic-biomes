package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class SeasonAdvancementService {
    private final SeasonCalendar calendar;
    private final SeasonStateRepository repository;
    private final CachedCurrentSeasonQuery currentSeasonQuery;

    public SeasonAdvancementService(
        SeasonCalendar calendar,
        SeasonStateRepository repository,
        CachedCurrentSeasonQuery currentSeasonQuery
    ) {
        this.calendar = calendar;
        this.repository = repository;
        this.currentSeasonQuery = currentSeasonQuery;
    }

    public SeasonId advance() {
        SeasonId currentSeason = currentSeasonQuery.currentSeason();
        SeasonId nextSeason = calendar.nextAfter(currentSeason);
        repository.saveCurrentSeason(nextSeason);
        currentSeasonQuery.updateCurrentSeason(nextSeason);
        return nextSeason;
    }
}
