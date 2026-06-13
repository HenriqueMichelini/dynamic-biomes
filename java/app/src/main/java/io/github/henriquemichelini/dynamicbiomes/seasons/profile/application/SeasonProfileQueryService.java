package io.github.henriquemichelini.dynamicbiomes.seasons.profile.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;

public final class SeasonProfileQueryService {
    private final CurrentSeasonQuery currentSeasonQuery;
    private final SeasonProfileProvider seasonProfileProvider;

    public SeasonProfileQueryService(
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider
    ) {
        this.currentSeasonQuery = currentSeasonQuery;
        this.seasonProfileProvider = seasonProfileProvider;
    }

    public SeasonProfile currentProfile() {
        SeasonId currentSeason = currentSeasonQuery.currentSeason();
        return seasonProfileProvider.profileFor(currentSeason);
    }
}
