package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Optional;

public interface SeasonStateRepository {
    Optional<SeasonId> findCurrentSeason();

    void saveCurrentSeason(SeasonId seasonId);
}
