package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService.SupportedBiome;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService.UnsupportedBiome;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropOreRule;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreInspectionServiceTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final OreKind COPPER_ORE = new OreKind("minecraft:copper_ore");
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        12,
        64,
        -8
    );
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        )
    );

    @Test
    void reportsPlayerPlacedOreWithoutMutatingItsOrigin() {
        ReadOnlyOriginRepository repository = new ReadOnlyOriginRepository(
            Optional.of(new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED))
        );

        SupportedBiome result = (SupportedBiome) service(repository).inspect(POSITION, IRON_ORE);

        assertEquals(new SupportedBiome(FOREST, true, OreOriginType.PLAYER_PLACED), result);
        assertFalse(result.eligibleForMultiplier());
        assertEquals(1, repository.reads);
    }

    @Test
    void reportsNaturalOreEligibilityOnlyWhenRuleExists() {
        OreInspectionService service = service(new ReadOnlyOriginRepository(Optional.empty()));

        SupportedBiome supported = (SupportedBiome) service.inspect(POSITION, IRON_ORE);
        SupportedBiome unsupported = (SupportedBiome) service.inspect(POSITION, COPPER_ORE);

        assertEquals(new SupportedBiome(FOREST, true, OreOriginType.NATURAL), supported);
        assertTrue(supported.eligibleForMultiplier());
        assertEquals(new SupportedBiome(FOREST, false, OreOriginType.NATURAL), unsupported);
        assertFalse(unsupported.eligibleForMultiplier());
    }

    @Test
    void unsupportedBiomeSkipsPolicyAndOriginReads() {
        ReadOnlyOriginRepository repository = new ReadOnlyOriginRepository(Optional.empty());
        OreInspectionService service = new OreInspectionService(
            position -> { throw new UnsupportedBiomeException("Unknown biome"); },
            biomeId -> { throw new AssertionError("Policy must be skipped"); },
            new OreOriginTrackingService(repository)
        );

        assertEquals(new UnsupportedBiome(Optional.empty()), service.inspect(POSITION, IRON_ORE));
        assertEquals(0, repository.reads);
    }

    @Test
    void unexpectedPolicyFailuresPropagate() {
        OreInspectionService service = new OreInspectionService(
            position -> FOREST_CONTEXT,
            biomeId -> { throw new IllegalStateException("Configuration corrupt"); },
            new OreOriginTrackingService(new ReadOnlyOriginRepository(Optional.empty()))
        );

        assertThrows(IllegalStateException.class, () -> service.inspect(POSITION, IRON_ORE));
    }

    private static OreInspectionService service(ReadOnlyOriginRepository repository) {
        return new OreInspectionService(
            position -> {
                assertEquals(POSITION, position);
                return FOREST_CONTEXT;
            },
            biomeId -> new OreDropPolicy(
                biomeId,
                Map.of(IRON_ORE, new OreDropOreRule(new OreDropMultiplierRange(1.0, 2.0), Map.of()))
            ),
            new OreOriginTrackingService(repository)
        );
    }

    private static final class ReadOnlyOriginRepository implements OreOriginRepository {
        private final Optional<OreOrigin> origin;
        private int reads;

        private ReadOnlyOriginRepository(Optional<OreOrigin> origin) {
            this.origin = origin;
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            assertEquals(POSITION, position);
            reads++;
            return origin;
        }

        @Override
        public void save(OreOrigin origin) {
            throw new AssertionError("Inspection must not save an origin");
        }

        @Override
        public void removeByPosition(BlockPosition position) {
            throw new AssertionError("Inspection must not remove an origin");
        }

        @Override
        public void applyChanges(
            Collection<BlockPosition> removedPositions,
            Collection<OreOrigin> savedOrigins
        ) {
            throw new AssertionError("Inspection must not change origins");
        }
    }
}
