package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public final class CachedCurrentSeasonQuery implements CurrentSeasonQuery {
    private SeasonId currentSeason;

    public CachedCurrentSeasonQuery(SeasonId initialSeason) {
        if (initialSeason == null) {
            throw new IllegalArgumentException(
                "Initial current season must not be null"
            );
        }
        this.currentSeason = initialSeason;
    }

    @Override
    public SeasonId currentSeason() {
        return currentSeason;
    }

    public void updateCurrentSeason(SeasonId seasonId) {
        if (seasonId == null) {
            throw new IllegalArgumentException(
                "Updated current season must not be null"
            );
        }
        this.currentSeason = seasonId;
    }
}
