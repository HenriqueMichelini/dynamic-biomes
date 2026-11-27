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

    public synchronized SeasonId advance() {
        return persist(calendar.nextAfter(currentSeasonQuery.currentSeason()));
    }

    public synchronized SeasonId setCurrentSeason(SeasonId season) {
        calendar.requireSeason(season);
        return persist(season);
    }

    private SeasonId persist(SeasonId season) {
        repository.saveCurrentSeason(season);
        currentSeasonQuery.updateCurrentSeason(season);
        return season;
    }
}
