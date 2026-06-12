package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Optional;

public interface OreOriginRepository {
    void save(OreOrigin origin);

    Optional<OreOrigin> findByPosition(BlockPosition position);

    void removeByPosition(BlockPosition position);
}
