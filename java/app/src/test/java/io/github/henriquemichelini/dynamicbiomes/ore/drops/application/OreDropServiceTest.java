package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreDropServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(
            UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")
        ),
        10,
        64,
        -20
    );

    @Test
    void appliesSelectedMultiplierToEligibleVanillaFortuneQuantity() {
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            new OreDropQuantityCalculator(() -> 0.49)
        );

        assertEquals(5, service.calculateDrops(POSITION, 3, 1.5));
    }

    @Test
    void preservesVanillaFortuneQuantityForPlayerPlacedOre() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        OreOriginTrackingService originTracking =
            new OreOriginTrackingService(repository);
        originTracking.recordPlayerPlacedOre(POSITION);
        OreDropService service = new OreDropService(
            originTracking,
            new OreDropQuantityCalculator(() -> {
                throw new AssertionError(
                    "Quantity calculation is unnecessary for ineligible ore"
                );
            })
        );

        assertEquals(3, service.calculateDrops(POSITION, 3, 2.0));
    }

    @Test
    void returnsZeroForZeroVanillaFortuneQuantity() {
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            new OreDropQuantityCalculator(() -> {
                throw new AssertionError(
                    "Variation is unnecessary for zero quantity"
                );
            })
        );

        assertEquals(0, service.calculateDrops(POSITION, 0, 1.5));
    }

    private static OreDropService serviceWith(
        OreOriginRepository repository,
        OreDropQuantityCalculator quantityCalculator
    ) {
        return new OreDropService(
            new OreOriginTrackingService(repository),
            quantityCalculator
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
