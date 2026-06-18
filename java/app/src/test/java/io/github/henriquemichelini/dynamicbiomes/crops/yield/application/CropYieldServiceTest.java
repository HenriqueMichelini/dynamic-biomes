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
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldBiomeFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldClimateFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEffectiveMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEnvironmentalFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
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
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
            new Fertility(0.5),
            new MineralRichness(0.3),
            new EcologicalPressure(0.2)
        )
    );

    @Test
    void appliesResolvedPolicySeasonalFactorAndNeutralEnvironmentalFactors() {
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
            seasonId -> seasonProfile(seasonId, 0.0, 0.0)
        );

        assertEquals(6, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 2));
    }

    @Test
    void appliesHighBiomeFertilityFactor() {
        CropYieldService service = serviceWith(
            position -> biomeContext(FOREST, 0.4, 0.8, 1.0, 0.3, 0.2),
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, 0.0, 0.0)
        );

        assertEquals(120, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void appliesLowBiomeFertilityFactor() {
        CropYieldService service = serviceWith(
            position -> biomeContext(FOREST, 0.4, 0.8, 0.0, 0.3, 0.2),
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, 0.0, 0.0)
        );

        assertEquals(80, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void appliesPositiveSeasonClimateFactor() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, 1.0, 1.0)
        );

        assertEquals(115, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void appliesNegativeSeasonClimateFactor() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, -1.0, -1.0)
        );

        assertEquals(85, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
    }

    @Test
    void composesSelectedBaseSeasonalBiomeAndClimateFactors() {
        CropYieldService service = serviceWith(
            position -> biomeContext(FOREST, 0.4, 0.8, 1.0, 0.3, 0.2),
            policyWith(
                new CropYieldMultiplierRange(1.25, 1.25),
                Map.of(SPRING, new CropYieldSeasonalFactor(1.10))
            ),
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, 0.0, 0.0)
        );

        assertEquals(165, service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 100));
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
            () -> SPRING,
            seasonId -> seasonProfile(seasonId, 0.0, 0.0)
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3)
        );
    }

    @Test
    void propagatesMissingSeasonProfilesAsConfigurationFailures() {
        CropYieldService service = serviceWith(
            position -> FOREST_CONTEXT,
            policyWith(new CropYieldMultiplierRange(1.0, 1.0), Map.of()),
            () -> SPRING,
            seasonId -> {
                throw new IllegalArgumentException("missing season profile");
            }
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.calculateProduceQuantity(POSITION, CropKind.WHEAT, 3)
        );
    }

    private static CropYieldService serviceWith(
        BiomeResolver biomeResolver,
        CropYieldPolicy policy,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider
    ) {
        return new CropYieldService(
            biomeResolver,
            biomeId -> policy,
            currentSeasonQuery,
            seasonProfileProvider,
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0),
            new CropYieldBiomeFactorCalculator(),
            new CropYieldClimateFactorCalculator(),
            new CropYieldEnvironmentalFactorCalculator(),
            new CropYieldEffectiveMultiplierCalculator()
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
