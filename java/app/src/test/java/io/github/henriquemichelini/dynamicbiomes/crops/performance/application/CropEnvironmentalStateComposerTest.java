package io.github.henriquemichelini.dynamicbiomes.crops.performance.application;

import static org.junit.jupiter.api.Assertions.assertAll;
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
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CropEnvironmentalStateComposerTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("69b0ff07-5875-4a08-baf1-b263ec2e573a")),
        5,
        64,
        -11
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");

    @Test
    void composesHumidityAndTemperatureFromBiomeVariablesAndSeasonFactors() {
        CropEnvironmentalStateComposer composer = composerWith(
            position -> biomeContext(FOREST, 0.80, 0.40, 0.65),
            () -> SUMMER,
            seasonId -> seasonProfile(seasonId, -0.50, 0.50)
        );

        CropEnvironmentalState state = composer.compose(POSITION);

        assertAll(
            () -> assertEquals(1.00, state.humidity().normalized()),
            () -> assertEquals(0.20, state.temperature().normalized())
        );
    }

    @Test
    void usesBiomeFertilityOnlyForSoilFertility() {
        CropEnvironmentalStateComposer composer = composerWith(
            position -> biomeContext(FOREST, 0.30, 0.30, 0.70),
            () -> SUMMER,
            seasonId -> seasonProfile(seasonId, 1.00, 1.00)
        );

        CropEnvironmentalState state = composer.compose(POSITION);

        assertEquals(0.70, state.soilFertility().normalized());
    }

    @Test
    void usesNeutralDefaultsForUnavailableEnvironmentalContracts() {
        CropEnvironmentalStateComposer composer = composerWith(
            position -> biomeContext(FOREST, 0.30, 0.30, 0.70),
            () -> SUMMER,
            seasonId -> seasonProfile(seasonId, 0.00, 0.00)
        );

        CropEnvironmentalState state = composer.compose(POSITION);

        assertAll(
            () -> assertEquals(0.50, state.windSpeed().normalized()),
            () -> assertEquals(0.50, state.rainStrength().normalized()),
            () -> assertEquals(0.50, state.solarIncidence().normalized())
        );
    }

    @Test
    void propagatesUnsupportedBiome() {
        CropEnvironmentalStateComposer composer = composerWith(
            position -> {
                throw new UnsupportedBiomeException("unsupported");
            },
            () -> {
                throw new AssertionError("Season query should be skipped");
            },
            seasonId -> {
                throw new AssertionError("Season profile lookup should be skipped");
            }
        );

        assertThrows(UnsupportedBiomeException.class, () -> composer.compose(POSITION));
    }

    @Test
    void propagatesRealUpstreamFailures() {
        CropEnvironmentalStateComposer composer = composerWith(
            position -> biomeContext(FOREST, 0.30, 0.30, 0.70),
            () -> SUMMER,
            seasonId -> {
                throw new IllegalStateException("season profile failed");
            }
        );

        assertThrows(IllegalStateException.class, () -> composer.compose(POSITION));
    }

    private static CropEnvironmentalStateComposer composerWith(
        BiomeResolver biomeResolver,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider
    ) {
        return new CropEnvironmentalStateComposer(
            biomeResolver,
            currentSeasonQuery,
            seasonProfileProvider
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
                new Fertility(fertility),
                new MineralRichness(0.20),
                new EcologicalPressure(0.30)
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
}
