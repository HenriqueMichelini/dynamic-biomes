package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Optional;

public interface SeasonStateRepository {
    /** @throws SeasonStatePersistenceException if storage cannot complete the read */
    Optional<SeasonId> findCurrentSeason();

    /** @throws SeasonStatePersistenceException if storage cannot complete the write */
    void saveCurrentSeason(SeasonId seasonId);
}
