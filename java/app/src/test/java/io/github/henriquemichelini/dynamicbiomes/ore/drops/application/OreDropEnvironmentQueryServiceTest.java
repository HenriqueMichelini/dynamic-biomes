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
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreDropEnvironmentQueryServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
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
    private static final SeasonId SPRING_ID = new SeasonId("minecraft:spring");
    private static final SeasonProfile SPRING_PROFILE = new SeasonProfile(
        SPRING_ID,
        new SeasonClimateAdjustment(
            new SeasonalAdjustment(0.2),
            new SeasonalAdjustment(0.3)
        )
    );

    @Test
    void resolvesBiomeContextAndSeasonProfileForPosition() {
        RecordingBiomeResolver biomeResolver = new RecordingBiomeResolver(FOREST_CONTEXT);
        OreDropEnvironmentQueryService service = new OreDropEnvironmentQueryService(
            biomeResolver,
            () -> SPRING_ID,
            seasonId -> SPRING_PROFILE
        );

        OreDropEnvironmentContext result = service.resolve(POSITION);

        assertEquals(FOREST_CONTEXT, result.biomeContext());
        assertEquals(SPRING_PROFILE, result.seasonProfile());
        assertEquals(POSITION, biomeResolver.resolvedPosition);
    }

    @Test
    void propagatesBiomeResolutionFailure() {
        OreDropEnvironmentQueryService service = new OreDropEnvironmentQueryService(
            position -> {
                throw new IllegalArgumentException("Unknown biome at position");
            },
            () -> SPRING_ID,
            seasonId -> SPRING_PROFILE
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.resolve(POSITION)
        );

        assertEquals("Unknown biome at position", exception.getMessage());
    }

    @Test
    void propagatesCurrentSeasonQueryFailure() {
        OreDropEnvironmentQueryService service = new OreDropEnvironmentQueryService(
            position -> FOREST_CONTEXT,
            () -> {
                throw new IllegalStateException("Current season is not initialized");
            },
            seasonId -> {
                throw new AssertionError(
                    "Provider should not be called when season query fails"
                );
            }
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> service.resolve(POSITION)
        );

        assertEquals("Current season is not initialized", exception.getMessage());
    }

    @Test
    void propagatesSeasonProfileProviderFailure() {
        OreDropEnvironmentQueryService service = new OreDropEnvironmentQueryService(
            position -> FOREST_CONTEXT,
            () -> SPRING_ID,
            seasonId -> {
                throw new IllegalArgumentException("Missing season profile: " + seasonId.value());
            }
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.resolve(POSITION)
        );

        assertEquals("Missing season profile: " + SPRING_ID.value(), exception.getMessage());
    }

    private static final class RecordingBiomeResolver
        implements io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver {
        private final BiomeContext context;
        private BlockPosition resolvedPosition;

        private RecordingBiomeResolver(BiomeContext context) {
            this.context = context;
        }

        @Override
        public BiomeContext resolve(BlockPosition position) {
            resolvedPosition = position;
            return context;
        }
    }
}
