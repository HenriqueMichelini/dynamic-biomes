package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.EcologicalPressure;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.MineralRichness;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
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
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
            new Fertility(0.7),
            new MineralRichness(0.3),
            new EcologicalPressure(0.2)
        )
    );
    private static final SeasonProfile SPRING_PROFILE = new SeasonProfile(
        new SeasonId("minecraft:spring"),
        new SeasonClimateAdjustment(
            new SeasonalAdjustment(0.2),
            new SeasonalAdjustment(0.3)
        )
    );

    @Test
    void appliesResolvedBiomePolicyAndOreKindToEligibleQuantity() {
        BlockPosition[] capturedPosition = new BlockPosition[1];
        OreDropEnvironmentQueryService environmentQuery = fixedEnvironmentQuery(
            FOREST_CONTEXT,
            SPRING_PROFILE,
            capturedPosition
        );
        RecordingPolicyProvider policyProvider = new RecordingPolicyProvider(
            new OreDropPolicy(
                FOREST,
                Map.of(IRON_ORE, new OreDropMultiplierRange(1.5, 1.5))
            )
        );
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            environmentQuery,
            policyProvider
        );

        assertEquals(5, service.calculateDrops(POSITION, IRON_ORE, 3));
        assertEquals(POSITION, capturedPosition[0]);
        assertEquals(FOREST, policyProvider.requestedBiomeId);
    }

    @Test
    void preservesQuantityAndSkipsEnvironmentResolutionForPlayerPlacedOre() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        OreOriginTrackingService originTracking = new OreOriginTrackingService(repository);
        originTracking.recordPlayerPlacedOre(POSITION);
        OreDropService service = new OreDropService(
            originTracking,
            new OreDropEnvironmentQueryService(
                position -> {
                    throw new AssertionError("Environment query is unnecessary for ineligible ore");
                },
                () -> {
                    throw new AssertionError("Season query is unnecessary for ineligible ore");
                },
                seasonId -> {
                    throw new AssertionError("Profile provider is unnecessary for ineligible ore");
                }
            ),
            biomeId -> {
                throw new AssertionError("Policy lookup is unnecessary for ineligible ore");
            },
            new OreDropMultiplierCalculator(() -> {
                throw new AssertionError("Multiplier selection is unnecessary for ineligible ore");
            }),
            new OreDropQuantityCalculator(() -> {
                throw new AssertionError("Quantity calculation is unnecessary for ineligible ore");
            })
        );

        assertEquals(3, service.calculateDrops(POSITION, IRON_ORE, 3));
    }

    @Test
    void propagatesMissingBiomePolicyFailure() {
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            fixedEnvironmentQuery(FOREST_CONTEXT, SPRING_PROFILE, null),
            biomeId -> {
                throw new IllegalArgumentException(
                    "Missing ore drop policy for biome: " + biomeId.value()
                );
            }
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.calculateDrops(POSITION, IRON_ORE, 3)
        );

        assertEquals(
            "Missing ore drop policy for biome: minecraft:forest",
            exception.getMessage()
        );
    }

    @Test
    void propagatesMissingOreRuleFailure() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(
                new OreKind("minecraft:gold_ore"),
                new OreDropMultiplierRange(1.0, 1.0)
            )
        );
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            fixedEnvironmentQuery(FOREST_CONTEXT, SPRING_PROFILE, null),
            biomeId -> policy
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.calculateDrops(POSITION, IRON_ORE, 3)
        );
    }

    @Test
    void propagatesEnvironmentQueryFailure() {
        OreDropService service = serviceWith(
            new InMemoryOreOriginRepository(),
            new OreDropEnvironmentQueryService(
                position -> {
                    throw new IllegalStateException("Current season is not initialized");
                },
                () -> {
                    throw new AssertionError("Query should not be called when resolver fails");
                },
                seasonId -> {
                    throw new AssertionError("Provider should not be called when resolver fails");
                }
            ),
            biomeId -> {
                throw new AssertionError("Policy lookup should not be called when query fails");
            }
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.calculateDrops(POSITION, IRON_ORE, 3)
        );

        assertEquals("Current season is not initialized", exception.getMessage());
    }

    private static OreDropService serviceWith(
        OreOriginRepository repository,
        OreDropEnvironmentQueryService environmentQuery,
        OreDropPolicyProvider policyProvider
    ) {
        return new OreDropService(
            new OreOriginTrackingService(repository),
            environmentQuery,
            policyProvider,
            new OreDropMultiplierCalculator(() -> 0.5),
            new OreDropQuantityCalculator(() -> 0.49)
        );
    }

    private static OreDropEnvironmentQueryService fixedEnvironmentQuery(
        BiomeContext biomeContext,
        SeasonProfile seasonProfile,
        BlockPosition[] capturedPosition
    ) {
        return new OreDropEnvironmentQueryService(
            position -> {
                if (capturedPosition != null && capturedPosition.length > 0) {
                    capturedPosition[0] = position;
                }
                return biomeContext;
            },
            () -> seasonProfile.seasonId(),
            seasonId -> seasonProfile
        );
    }

    private static final class RecordingPolicyProvider implements OreDropPolicyProvider {
        private final OreDropPolicy policy;
        private BiomeId requestedBiomeId;

        private RecordingPolicyProvider(OreDropPolicy policy) {
            this.policy = policy;
        }

        @Override
        public OreDropPolicy policyFor(BiomeId biomeId) {
            requestedBiomeId = biomeId;
            return policy;
        }
    }

    private static final class InMemoryOreOriginRepository
        implements OreOriginRepository {
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
