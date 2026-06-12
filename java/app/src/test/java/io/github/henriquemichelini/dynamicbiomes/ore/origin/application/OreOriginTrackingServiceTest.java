package io.github.henriquemichelini.dynamicbiomes.ore.origin.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreOriginTrackingServiceTest {

    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(
            UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")
        ),
        10,
        64,
        -20
    );

    @Test
    void recordingPlayerPlacedOreStoresIneligibleOrigin() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        OreOriginTrackingService service = new OreOriginTrackingService(
            repository
        );

        service.recordPlayerPlacedOre(POSITION);

        OreOrigin storedOrigin = repository
            .findByPosition(POSITION)
            .orElseThrow();
        assertAll(
            () ->
                assertEquals(
                    new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED),
                    storedOrigin
                ),
            () -> assertFalse(storedOrigin.isEligibleForBiomeBasedMultiplier())
        );
    }

    @Test
    void knownPlayerPlacedOreIsNotEligibleForBiomeMultiplier() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED));
        OreOriginTrackingService service = new OreOriginTrackingService(
            repository
        );

        assertFalse(service.isEligibleForBiomeMultiplier(POSITION));
    }

    @Test
    void unknownOreIsEligibleForBiomeMultiplierByDefault() {
        OreOriginTrackingService service = new OreOriginTrackingService(
            new InMemoryOreOriginRepository()
        );

        assertTrue(service.isEligibleForBiomeMultiplier(POSITION));
    }

    @Test
    void knownNaturalOreUsesDomainEligibility() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.NATURAL));
        OreOriginTrackingService service = new OreOriginTrackingService(
            repository
        );

        assertTrue(service.isEligibleForBiomeMultiplier(POSITION));
    }

    private static final class InMemoryOreOriginRepository
        implements OreOriginRepository
    {

        private final Map<BlockPosition, OreOrigin> origins = new HashMap<>();

        @Override
        public void save(OreOrigin origin) {
            origins.put(origin.position(), origin);
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            return Optional.ofNullable(origins.get(position));
        }
    }
}
