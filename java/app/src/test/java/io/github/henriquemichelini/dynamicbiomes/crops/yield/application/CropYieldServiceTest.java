package io.github.henriquemichelini.dynamicbiomes.crops.yield.application;

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
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.CropPerformanceService;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class CropYieldServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
        10,
        64,
        -20
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final BiomeId DESERT = new BiomeId("minecraft:desert");
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
            new Fertility(0.5)
        )
    );

    @Test
    void appliesResolvedPolicySeasonalFactorAndNeutralCropPerformance() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            new CropYieldPolicy(
                FOREST,
                Map.of(
                    CropKind.WHEAT,
                    new CropYieldCropRule(
                        new CropYieldMultiplierRange(2.0, 2.0),
                        Map.of(SPRING, new CropYieldSeasonalFactor(1.5))
                    )
                )
            ),
            () -> SPRING,
            neutralPerformance()
        );

        assertEquals(6, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2));
    }

    @Test
    void usesResolvedBiomePolicyMultiplier() {
        CropYieldService service = serviceWith(
            position -> biomeContext(DESERT, 0.4, 0.8, 0.5),
            biomeId -> {
                if (FOREST.equals(biomeId)) {
                    return new CropYieldPolicy(
                        FOREST,
                        Map.of(
                            CropKind.WHEAT,
                            new CropYieldCropRule(
                                new CropYieldMultiplierRange(2.0, 2.0),
                                Map.of()
                            )
                        )
                    );
                }
                if (DESERT.equals(biomeId)) {
                    return new CropYieldPolicy(
                        DESERT,
                        Map.of(
                            CropKind.WHEAT,
                            new CropYieldCropRule(
                                new CropYieldMultiplierRange(0.5, 0.5),
                                Map.of()
                            )
                        )
                    );
                }
                throw new UnsupportedCropYieldPolicyException("missing");
            },
            () -> SPRING,
            neutralPerformance()
        );

        assertEquals(50, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void doesNotApplyFertilityDerivedBiomeFactor() {
        CropYieldService service = serviceWith(
            position -> biomeContext(FOREST, 0.4, 0.8, 1.0),
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            neutralPerformance()
        );

        assertEquals(100, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void doesNotApplyYieldOwnedSeasonClimateFactorAfterPerformanceIsWired() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            neutralPerformance()
        );

        assertEquals(100, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void appliesCropPerformanceHarvestQuantityFactor() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            performance(2.0)
        );

        assertEquals(200, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void treatsMissingCropPerformanceProfileAsNeutral() {
        CropPerformanceService performanceService = new CropPerformanceService(
            position -> neutralEnvironmentalState(),
            cropKind -> {
                throw new UnsupportedCropPerformanceProfileException("missing");
            },
            new CropPerformanceCalculator()
        );

        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            performanceService::performanceFor
        );

        assertEquals(100, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void composesSelectedBiomeCropSeasonalAndPerformanceFactors() {
        CropYieldService service = serviceWith(
            position -> biomeContext(FOREST, 0.4, 0.8, 1.0),
            policyWith(
                new CropYieldMultiplierRange(1.25, 1.25),
                Map.of(SPRING, new CropYieldSeasonalFactor(1.10))
            ),
            () -> SPRING,
            performance(2.0)
        );

        assertEquals(275, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void preservesVanillaForUnsupportedBiomeAndSkipsPolicyAndSeason() {
        CropYieldService service = new CropYieldService(
            position -> {
                throw new UnsupportedBiomeException("unsupported");
            },
            biomeId -> {
                throw new AssertionError("Policy lookup should be skipped");
            },
            () -> {
                throw new AssertionError("Season query should be skipped");
            },
            (position, cropKind) -> {
                throw new AssertionError("Crop performance should be skipped");
            },
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );

        assertEquals(3, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3));
    }

    @Test
    void preservesVanillaForMissingYieldPolicyAndSkipsSeason() {
        CropYieldService service = new CropYieldService(
            position -> FOREST_CONTEXT,
            biomeId -> {
                throw new UnsupportedCropYieldPolicyException("missing");
            },
            () -> {
                throw new AssertionError("Season query should be skipped");
            },
            (position, cropKind) -> {
                throw new AssertionError("Crop performance should be skipped");
            },
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );

        assertEquals(3, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3));
    }

    @Test
    void propagatesRealFailures() {
        CropYieldService service = serviceWith(
            position -> {
                throw new IllegalStateException("resolver failed");
            },
            new CropYieldPolicy(
                FOREST,
                Map.of(
                    CropKind.WHEAT,
                    new CropYieldCropRule(new CropYieldMultiplierRange(1.0, 1.0), Map.of())
                )
            ),
            () -> SPRING,
            neutralPerformance()
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3)
        );
    }

    @Test
    void propagatesCropPerformanceFailuresAfterYieldPolicySupportIsEstablished() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            (position, cropKind) -> {
                throw new IllegalStateException("performance failed");
            }
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3)
        );
    }

    private static CropYieldService serviceWith(
        BiomeResolver biomeResolver,
        CropYieldPolicy policy,
        CurrentSeasonQuery currentSeasonQuery,
        BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery
    ) {
        return new CropYieldService(
            biomeResolver,
            biomeId -> policy,
            currentSeasonQuery,
            cropPerformanceQuery,
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );
    }

    private static CropYieldService serviceWith(
        BiomeResolver biomeResolver,
        CropYieldPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery
    ) {
        return new CropYieldService(
            biomeResolver,
            policyProvider,
            currentSeasonQuery,
            cropPerformanceQuery,
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );
    }

    private static CropYieldPolicy policyWith(
        CropYieldMultiplierRange multiplierRange,
        Map<SeasonId, CropYieldSeasonalFactor> seasonalFactors
    ) {
        return new CropYieldPolicy(
            FOREST,
            Map.of(
                CropKind.WHEAT,
                new CropYieldCropRule(multiplierRange, seasonalFactors)
            )
        );
    }

    private static BiomeContext biomeContext(
        BiomeId biomeId,
        double humidity,
        double temperature,
        double fertility
    ) {
        return new BiomeContext(
            biomeId,
            new BiomeProfile(
                biomeId,
                new ClimateProfile(new Humidity(humidity), new Temperature(temperature)),
                new Fertility(fertility)
            )
        );
    }

    private static BiFunction<BlockPosition, CropKind, CropPerformanceResult> neutralPerformance() {
        return performance(1.0);
    }

    private static BiFunction<BlockPosition, CropKind, CropPerformanceResult> performance(
        double harvestQuantityFactor
    ) {
        CropPerformanceResult result = new CropPerformanceResult(
            OptionalDouble.empty(),
            1.0,
            1.0,
            harvestQuantityFactor
        );
        return (position, cropKind) -> result;
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
}
