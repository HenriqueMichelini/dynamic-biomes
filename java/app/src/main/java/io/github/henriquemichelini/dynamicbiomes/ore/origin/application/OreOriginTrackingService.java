package io.github.henriquemichelini.dynamicbiomes.ore.origin.application;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class OreOriginTrackingService {
    private final OreOriginRepository repository;

    public OreOriginTrackingService(OreOriginRepository repository) {
        this.repository = repository;
    }

    public void recordPlayerPlacedOre(BlockPosition position) {
        repository.save(new OreOrigin(position, OreOriginType.PLAYER_PLACED));
    }

    public boolean isEligibleForBiomeMultiplier(BlockPosition position) {
        return repository.findByPosition(position)
            .map(OreOrigin::isEligibleForBiomeBasedMultiplier)
            .orElse(true);
    }

    public void clearTrackedOrigin(BlockPosition position) {
        repository.removeByPosition(position);
    }
}
