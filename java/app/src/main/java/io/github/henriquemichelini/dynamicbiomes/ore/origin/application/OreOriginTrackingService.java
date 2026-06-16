package io.github.henriquemichelini.dynamicbiomes.ore.origin.application;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Optional;

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

    public Optional<OreOrigin> originAt(BlockPosition position) {
        return repository.findByPosition(position);
    }

    public void clearTrackedOrigin(BlockPosition position) {
        repository.removeByPosition(position);
    }

    public void moveTrackedOrigin(BlockPosition from, BlockPosition to) {
        repository.findByPosition(from).ifPresent(origin -> {
            repository.removeByPosition(from);
            repository.save(new OreOrigin(to, origin.type()));
        });
    }
}
