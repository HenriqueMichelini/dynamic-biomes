package io.github.henriquemichelini.dynamicbiomes.crops.yield.application;

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
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CropYieldServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
        10,
        64,
        -20
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
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

    @Test
    void appliesResolvedPolicyAndSeasonalFactor() {
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
            () -> SPRING
        );

        assertEquals(6, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2));
    }

    @Test
    void ignoresSeasonProfileClimateAdjustments() {
        SeasonProfile freezingWinter = seasonProfile(WINTER, -1.0, -1.0);
        SeasonProfile hotWetWinter = seasonProfile(WINTER, 1.0, 1.0);
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            new CropYieldPolicy(
                FOREST,
                Map.of(
                    CropKind.WHEAT,
                    new CropYieldCropRule(
                        new CropYieldMultiplierRange(2.0, 2.0),
                        Map.of(WINTER, new CropYieldSeasonalFactor(1.5))
                    )
                )
            ),
            () -> WINTER
        );

        assertEquals(6, calculateWithIgnoredSeasonProfile(service, freezingWinter));
        assertEquals(6, calculateWithIgnoredSeasonProfile(service, hotWetWinter));
    }

    @Test
    void ignoresBiomeProfileClimateAndEnvironmentValuesAfterResolvingBiomeId() {
        CropYieldPolicy policy = new CropYieldPolicy(
            FOREST,
            Map.of(
                CropKind.WHEAT,
                new CropYieldCropRule(
                    new CropYieldMultiplierRange(2.0, 2.0),
                    Map.of(SPRING, new CropYieldSeasonalFactor(1.5))
                )
            )
        );
        CropYieldService barrenColdForestService = serviceWith(
            position -> biomeContext(FOREST, 0.0, 0.0, 0.0, 0.0, 1.0),
            policy,
            () -> SPRING
        );
        CropYieldService fertileHotForestService = serviceWith(
            position -> biomeContext(FOREST, 1.0, 1.0, 1.0, 1.0, 0.0),
            policy,
            () -> SPRING
        );

        assertEquals(
            barrenColdForestService.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2),
            fertileHotForestService.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2)
        );
        assertEquals(
            6,
            fertileHotForestService.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2)
        );
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
            () -> SPRING
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3)
        );
    }

    private static CropYieldService serviceWith(
        BiomeResolver biomeResolver,
        CropYieldPolicy policy,
        io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery currentSeasonQuery
    ) {
        return new CropYieldService(
            biomeResolver,
            biomeId -> policy,
            currentSeasonQuery,
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );
    }

    private static int calculateWithIgnoredSeasonProfile(
        CropYieldService service,
        SeasonProfile ignoredSeasonProfile
    ) {
        assertEquals(WINTER, ignoredSeasonProfile.seasonId());
        return service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2);
    }

    private static SeasonProfile seasonProfile(
        SeasonId seasonId,
        double temperatureAdjustment,
        double humidityAdjustment
    ) {
        return new SeasonProfile(
            seasonId,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(temperatureAdjustment),
                new SeasonalAdjustment(humidityAdjustment)
            )
        );
    }

    private static BiomeContext biomeContext(
        BiomeId biomeId,
        double humidity,
        double temperature,
        double fertility,
        double mineralRichness,
        double ecologicalPressure
    ) {
        return new BiomeContext(
            biomeId,
            new BiomeProfile(
                biomeId,
                new ClimateProfile(new Humidity(humidity), new Temperature(temperature)),
                new Fertility(fertility),
                new MineralRichness(mineralRichness),
                new EcologicalPressure(ecologicalPressure)
            )
        );
    }
}
