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
    void originAtReadsTrackedOriginWithoutChangingRepository() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        OreOrigin origin = new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED);
        repository.save(origin);
        OreOriginTrackingService service = new OreOriginTrackingService(
            repository
        );

        assertEquals(Optional.of(origin), service.originAt(POSITION));
        assertEquals(Optional.of(origin), repository.findByPosition(POSITION));
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

    @Test
    void clearingTrackedOriginRestoresUnknownOreEligibility() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        OreOriginTrackingService service = new OreOriginTrackingService(
            repository
        );
        service.recordPlayerPlacedOre(POSITION);
        assertFalse(service.isEligibleForBiomeMultiplier(POSITION));

        service.clearTrackedOrigin(POSITION);

        assertTrue(service.isEligibleForBiomeMultiplier(POSITION));
    }

    @Test
    void clearingUnknownOriginIsSafe() {
        OreOriginTrackingService service = new OreOriginTrackingService(
            new InMemoryOreOriginRepository()
        );

        service.clearTrackedOrigin(POSITION);

        assertTrue(service.isEligibleForBiomeMultiplier(POSITION));
    }

    @Test
    void moveTrackedOriginTransfersPlayerPlacedToDestination() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED));
        OreOriginTrackingService service = new OreOriginTrackingService(repository);

        BlockPosition destination = new BlockPosition(
            POSITION.world(),
            POSITION.x() + 1,
            POSITION.y(),
            POSITION.z()
        );
        service.moveTrackedOrigin(POSITION, destination);

        assertAll(
            () -> assertTrue(repository.findByPosition(POSITION).isEmpty()),
            () -> assertEquals(
                new OreOrigin(destination, OreOriginType.PLAYER_PLACED),
                repository.findByPosition(destination).orElseThrow()
            ),
            () -> assertFalse(service.isEligibleForBiomeMultiplier(destination))
        );
    }

    @Test
    void moveTrackedOriginTransfersNaturalToDestination() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.NATURAL));
        OreOriginTrackingService service = new OreOriginTrackingService(repository);

        BlockPosition destination = new BlockPosition(
            POSITION.world(),
            POSITION.x(),
            POSITION.y() + 1,
            POSITION.z()
        );
        service.moveTrackedOrigin(POSITION, destination);

        assertAll(
            () -> assertTrue(repository.findByPosition(POSITION).isEmpty()),
            () -> assertEquals(
                new OreOrigin(destination, OreOriginType.NATURAL),
                repository.findByPosition(destination).orElseThrow()
            ),
            () -> assertTrue(service.isEligibleForBiomeMultiplier(destination))
        );
    }

    @Test
    void moveTrackedOriginForUntrackedOreDoesNothing() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        OreOriginTrackingService service = new OreOriginTrackingService(repository);

        BlockPosition destination = new BlockPosition(
            POSITION.world(),
            POSITION.x(),
            POSITION.y(),
            POSITION.z() + 1
        );
        service.moveTrackedOrigin(POSITION, destination);

        assertAll(
            () -> assertTrue(repository.findByPosition(POSITION).isEmpty()),
            () -> assertTrue(repository.findByPosition(destination).isEmpty())
        );
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

        @Override
        public void removeByPosition(BlockPosition position) {
            origins.remove(position);
        }
    }
}
