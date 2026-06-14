package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public interface CurrentSeasonQuery {
    SeasonId currentSeason();
}
