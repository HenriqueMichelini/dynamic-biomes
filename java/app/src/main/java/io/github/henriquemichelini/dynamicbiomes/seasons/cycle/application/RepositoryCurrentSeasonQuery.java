package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class RepositoryCurrentSeasonQuery implements CurrentSeasonQuery {
    private final SeasonStateRepository repository;

    public RepositoryCurrentSeasonQuery(SeasonStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public SeasonId currentSeason() {
        return repository.findCurrentSeason()
            .orElseThrow(() -> new IllegalStateException("Current season is not initialized"));
    }
}
