package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Collection;
import java.util.Optional;

/** Storage operations report I/O failures as {@link OreOriginPersistenceException}. */
public interface OreOriginRepository {
    void save(OreOrigin origin);

    Optional<OreOrigin> findByPosition(BlockPosition position);

    void removeByPosition(BlockPosition position);

    void applyChanges(Collection<BlockPosition> removedPositions, Collection<OreOrigin> savedOrigins);
}
