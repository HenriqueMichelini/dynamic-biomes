package io.github.henriquemichelini.dynamicbiomes.crops.growth.application;

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
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChanceVariationSource;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CropGrowthServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(
            UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")
        ),
        10,
        64,
        -20
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
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
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void allowsGrowthForFullConfiguredChance() {
        RecordingBiomeResolver biomeResolver = new RecordingBiomeResolver(FOREST_CONTEXT);
        RecordingPolicyProvider policyProvider = new RecordingPolicyProvider(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(SPRING);

        CropGrowthDecision decision = new CropGrowthService(
            biomeResolver,
            policyProvider,
            currentSeasonQuery
        ).decideNaturalWheatGrowth(POSITION);

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
        assertEquals(POSITION, biomeResolver.requestedPosition);
        assertEquals(FOREST, policyProvider.requestedBiomeId);
        assertEquals(1, currentSeasonQuery.queryCount);
    }

    @Test
    void cancelsGrowthForZeroConfiguredChance() {
        CropGrowthService service = serviceWith(
            policy(0.0, () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            })
        );

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void allowsGrowthWhenVariationIsLowerThanIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.49));

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void cancelsGrowthWhenVariationIsEqualToIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.5));

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void cancelsGrowthWhenVariationIsGreaterThanIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.51));

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void cancelsGrowthWithZeroSeasonalFactor() {
        CropGrowthService service = serviceWith(
            policy(
                0.5,
                Map.of(SPRING, new CropGrowthSeasonalFactor(0.0)),
                () -> 0.01
            ),
            SPRING
        );

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void allowsGrowthWithCappedSeasonalFactor() {
        CropGrowthService service = serviceWith(
            policy(
                0.5,
                Map.of(SUMMER, new CropGrowthSeasonalFactor(2.0)),
                () -> 1.0
            ),
            SUMMER
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void usesBaseChanceWhenCurrentSeasonHasNoFactor() {
        CropGrowthService service = serviceWith(
            policy(
                0.5,
                Map.of(SUMMER, new CropGrowthSeasonalFactor(2.0)),
                () -> 0.5
            ),
            WINTER
        );

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void allowsGrowthForUnsupportedBiomeToPreserveVanillaBehavior() {
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(SPRING);
        CropGrowthService service = new CropGrowthService(
            position -> {
                throw new UnsupportedBiomeException(
                    "Missing static biome profile for resolved biome: minecraft:ocean"
                );
            },
            biomeId -> {
                throw new AssertionError("Policy lookup should not run for unsupported biome");
            },
            currentSeasonQuery
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
        assertEquals(0, currentSeasonQuery.queryCount);
    }

    @Test
    void allowsGrowthForMissingWheatPolicyToPreserveVanillaBehavior() {
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(SPRING);
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> {
                throw new UnsupportedCropGrowthPolicyException(
                    "Missing wheat growth policy for biome: " + biomeId.value()
                );
            },
            currentSeasonQuery
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalWheatGrowth(POSITION)
        );
        assertEquals(0, currentSeasonQuery.queryCount);
    }

    @Test
    void propagatesBiomeResolverFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> {
                throw new IllegalStateException("Resolver failure");
            },
            biomeId -> {
                throw new AssertionError("Policy lookup should not run when resolver fails");
            },
            new RecordingCurrentSeasonQuery(SPRING)
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalWheatGrowth(POSITION)
        );

        assertEquals("Resolver failure", exception.getMessage());
    }

    @Test
    void propagatesPolicyProviderFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> {
                throw new IllegalStateException("Provider failure");
            },
            new RecordingCurrentSeasonQuery(SPRING)
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalWheatGrowth(POSITION)
        );

        assertEquals("Provider failure", exception.getMessage());
    }

    @Test
    void propagatesPolicyDecisionFailure() {
        CropGrowthService service = serviceWith(policy(0.5, () -> Double.NaN));

        assertThrows(
            IllegalArgumentException.class,
            () -> service.decideNaturalWheatGrowth(POSITION)
        );
    }

    @Test
    void propagatesCurrentSeasonQueryFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> policy(0.5, () -> 0.0),
            () -> {
                throw new IllegalStateException("Season query failure");
            }
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalWheatGrowth(POSITION)
        );

        assertEquals("Season query failure", exception.getMessage());
    }

    private static CropGrowthService serviceWith(CropGrowthPolicy policy) {
        return serviceWith(policy, SPRING);
    }

    private static CropGrowthService serviceWith(
        CropGrowthPolicy policy,
        SeasonId currentSeason
    ) {
        return new CropGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> policy,
            new RecordingCurrentSeasonQuery(currentSeason)
        );
    }

    private static CropGrowthPolicy policy(
        double chance,
        CropGrowthChanceVariationSource variationSource
    ) {
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            variationSource
        );
    }

    private static CropGrowthPolicy policy(
        double chance,
        Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors,
        CropGrowthChanceVariationSource variationSource
    ) {
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            seasonalFactors,
            variationSource
        );
    }

    private static final class RecordingBiomeResolver implements BiomeResolver {
        private final BiomeContext context;
        private BlockPosition requestedPosition;

        private RecordingBiomeResolver(BiomeContext context) {
            this.context = context;
        }

        @Override
        public BiomeContext resolve(BlockPosition position) {
            requestedPosition = position;
            return context;
        }
    }

    private static final class RecordingPolicyProvider
        implements CropGrowthPolicyProvider {

        private final CropGrowthPolicy policy;
        private BiomeId requestedBiomeId;

        private RecordingPolicyProvider(CropGrowthPolicy policy) {
            this.policy = policy;
        }

        @Override
        public CropGrowthPolicy policyFor(BiomeId biomeId) {
            requestedBiomeId = biomeId;
            return policy;
        }
    }

    private static final class RecordingCurrentSeasonQuery
        implements CurrentSeasonQuery {

        private final SeasonId currentSeason;
        private int queryCount;

        private RecordingCurrentSeasonQuery(SeasonId currentSeason) {
            this.currentSeason = currentSeason;
        }

        @Override
        public SeasonId currentSeason() {
            queryCount++;
            return currentSeason;
        }
    }
}
