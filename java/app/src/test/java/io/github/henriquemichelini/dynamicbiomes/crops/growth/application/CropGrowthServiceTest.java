package io.github.henriquemichelini.dynamicbiomes.crops.growth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChanceVariationSource;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.CropPerformanceService;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.OptionalDouble;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
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
            new Fertility(0.7)
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
            currentSeasonQuery,
            neutralPerformanceQuery()
        ).decideNaturalGrowth(POSITION, CropKind.WHEAT);

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
        assertEquals(POSITION, biomeResolver.requestedPosition);
        assertEquals(FOREST, policyProvider.requestedBiomeId);
        assertEquals(CropKind.WHEAT, policyProvider.requestedCropKind);
        assertEquals(1, currentSeasonQuery.queryCount);
    }

    @Test
    void passesRequestedCropKindToPolicyProvider() {
        RecordingPolicyProvider policyProvider = new RecordingPolicyProvider(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            policyProvider,
            new RecordingCurrentSeasonQuery(SPRING),
            neutralPerformanceQuery()
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.CARROTS)
        );
        assertEquals(FOREST, policyProvider.requestedBiomeId);
        assertEquals(CropKind.CARROTS, policyProvider.requestedCropKind);
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
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void allowsGrowthWhenVariationIsLowerThanIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.49));

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void cancelsGrowthWhenVariationIsEqualToIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.5));

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void cancelsGrowthWhenVariationIsGreaterThanIntermediateChance() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.51));

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
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
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
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
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
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
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void neutralCropPerformancePreservesCurrentDynamicBiomesGrowthBehavior() {
        CropGrowthService service = serviceWith(policy(0.5, () -> 0.5));

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void multipliesEffectiveGrowthChanceByCropPerformanceChanceFactor() {
        RecordingPerformanceQuery performanceQuery = new RecordingPerformanceQuery(
            0.5
        );
        CropGrowthService service = serviceWith(
            policy(0.8, () -> 0.4),
            SPRING,
            performanceQuery
        );

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
        assertEquals(POSITION, performanceQuery.requestedPosition);
        assertEquals(CropKind.WHEAT, performanceQuery.requestedCropKind);
    }

    @Test
    void missingCropPerformanceProfilePreservesCurrentDynamicBiomesGrowthBehavior() {
        CropGrowthService service = serviceWith(
            policy(0.5, () -> 0.5),
            SPRING,
            missingPerformanceProfileQuery()
        );

        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void allowsGrowthForUnsupportedBiomeToPreserveVanillaBehavior() {
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(SPRING);
        RecordingPerformanceQuery performanceQuery = new RecordingPerformanceQuery(
            0.5
        );
        CropGrowthService service = new CropGrowthService(
            position -> {
                throw new UnsupportedBiomeException(
                    "Missing static biome profile for resolved biome: minecraft:ocean"
                );
            },
            (biomeId, cropKind) -> {
                throw new AssertionError("Policy lookup should not run for unsupported biome");
            },
            currentSeasonQuery,
            performanceQuery
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
        assertEquals(0, currentSeasonQuery.queryCount);
        assertEquals(0, performanceQuery.queryCount);
    }

    @Test
    void allowsGrowthForMissingWheatPolicyToPreserveVanillaBehavior() {
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(SPRING);
        RecordingPerformanceQuery performanceQuery = new RecordingPerformanceQuery(
            0.5
        );
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            (biomeId, cropKind) -> {
                throw new UnsupportedCropGrowthPolicyException(
                    "Missing wheat growth policy for biome: " + biomeId.value()
                );
            },
            currentSeasonQuery,
            performanceQuery
        );

        assertEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
        assertEquals(0, currentSeasonQuery.queryCount);
        assertEquals(0, performanceQuery.queryCount);
    }

    @Test
    void propagatesBiomeResolverFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> {
                throw new IllegalStateException("Resolver failure");
            },
            (biomeId, cropKind) -> {
                throw new AssertionError("Policy lookup should not run when resolver fails");
            },
            new RecordingCurrentSeasonQuery(SPRING),
            neutralPerformanceQuery()
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );

        assertEquals("Resolver failure", exception.getMessage());
    }

    @Test
    void propagatesPolicyProviderFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            (biomeId, cropKind) -> {
                throw new IllegalStateException("Provider failure");
            },
            new RecordingCurrentSeasonQuery(SPRING),
            neutralPerformanceQuery()
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );

        assertEquals("Provider failure", exception.getMessage());
    }

    @Test
    void propagatesPolicyDecisionFailure() {
        CropGrowthService service = serviceWith(policy(0.5, () -> Double.NaN));

        assertThrows(
            IllegalArgumentException.class,
            () -> service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void propagatesCurrentSeasonQueryFailure() {
        CropGrowthService service = new CropGrowthService(
            position -> FOREST_CONTEXT,
            (biomeId, cropKind) -> policy(0.5, () -> 0.0),
            () -> {
                throw new IllegalStateException("Season query failure");
            },
            neutralPerformanceQuery()
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalGrowth(POSITION, CropKind.WHEAT)
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
        return serviceWith(policy, currentSeason, neutralPerformanceQuery());
    }

    private static CropGrowthService serviceWith(
        CropGrowthPolicy policy,
        SeasonId currentSeason,
        BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery
    ) {
        return new CropGrowthService(
            position -> FOREST_CONTEXT,
            (biomeId, cropKind) -> policy,
            new RecordingCurrentSeasonQuery(currentSeason),
            cropPerformanceQuery
        );
    }

    private static BiFunction<BlockPosition, CropKind, CropPerformanceResult> neutralPerformanceQuery() {
        return (position, cropKind) -> performanceResult(1.0);
    }

    private static BiFunction<BlockPosition, CropKind, CropPerformanceResult> missingPerformanceProfileQuery() {
        CropPerformanceService service = new CropPerformanceService(
            position -> neutralEnvironmentalState(),
            cropKind -> {
                throw new UnsupportedCropPerformanceProfileException(
                    "Missing crop performance profile for crop: " +
                    cropKind.policyKey()
                );
            },
            new CropPerformanceCalculator()
        );
        return service::performanceFor;
    }

    private static CropPerformanceResult performanceResult(double growthChanceFactor) {
        return new CropPerformanceResult(
            OptionalDouble.empty(),
            1.0,
            growthChanceFactor,
            1.0
        );
    }

    private static CropEnvironmentalState neutralEnvironmentalState() {
        NormalizedEnvironmentalValue neutral = new NormalizedEnvironmentalValue(0.5);
        return new CropEnvironmentalState(
            neutral,
            neutral,
            neutral,
            neutral,
            neutral,
            neutral
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
        private CropKind requestedCropKind;

        private RecordingPolicyProvider(CropGrowthPolicy policy) {
            this.policy = policy;
        }

        @Override
        public CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind) {
            requestedBiomeId = biomeId;
            requestedCropKind = cropKind;
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

    private static final class RecordingPerformanceQuery
        implements BiFunction<BlockPosition, CropKind, CropPerformanceResult> {

        private final double growthChanceFactor;
        private BlockPosition requestedPosition;
        private CropKind requestedCropKind;
        private int queryCount;

        private RecordingPerformanceQuery(double growthChanceFactor) {
            this.growthChanceFactor = growthChanceFactor;
        }

        @Override
        public CropPerformanceResult apply(
            BlockPosition position,
            CropKind cropKind
        ) {
            requestedPosition = position;
            requestedCropKind = cropKind;
            queryCount++;
            return performanceResult(growthChanceFactor);
        }
    }
}
