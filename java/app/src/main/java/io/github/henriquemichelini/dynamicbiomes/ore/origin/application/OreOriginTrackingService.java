package io.github.henriquemichelini.dynamicbiomes.ore.origin.application;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OreOriginTrackingService {
    private final OreOriginRepository repository;

    public OreOriginTrackingService(OreOriginRepository repository) {
        this.repository = repository;
    }

    public void recordPlayerPlacedOre(BlockPosition position) {
        repository.save(new OreOrigin(position, OreOriginType.PLAYER_PLACED));
    }

    public boolean isEligibleForBiomeMultiplier(BlockPosition position) {
        return originTypeAt(position).isEligibleForBiomeBasedMultiplier();
    }

    public OreOriginType originTypeAt(BlockPosition position) {
        return originAt(position).map(OreOrigin::type).orElse(OreOriginType.NATURAL);
    }

    public Optional<OreOrigin> originAt(BlockPosition position) {
        return repository.findByPosition(position);
    }

    public void clearTrackedOrigin(BlockPosition position) {
        repository.removeByPosition(position);
    }

    public void clearTrackedOrigins(Collection<BlockPosition> positions) {
        repository.applyChanges(positions, List.of());
    }

    public void moveTrackedOrigin(BlockPosition from, BlockPosition to) {
        moveTrackedOrigins(Map.of(from, to));
    }

    public void moveTrackedOrigins(Map<BlockPosition, BlockPosition> movements) {
        if (movements.isEmpty()) return;

        Set<BlockPosition> affectedPositions = new HashSet<>(movements.keySet());
        affectedPositions.addAll(movements.values());

        List<OreOrigin> transferredOrigins = new ArrayList<>();
        movements.forEach((source, destination) -> originAt(source).ifPresent(origin ->
            transferredOrigins.add(new OreOrigin(destination, origin.type()))
        ));
        repository.applyChanges(affectedPositions, transferredOrigins);
    }
}
