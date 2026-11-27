package io.github.henriquemichelini.dynamicbiomes.trees.growth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.UnsupportedTreeGrowthPolicyException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TreeGrowthServiceTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
        -10,
        64,
        20
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        )
    );
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void resolvesTheOriginBiomeAndUsesTheCurrentSeasonForEachAttempt() {
        AtomicReference<SeasonId> currentSeason = new AtomicReference<>(WINTER);
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.8),
            Map.of(WINTER, new TreeGrowthSeasonalFactor(0.5)),
            () -> 0.6
        );
        TreeGrowthService service = new TreeGrowthService(
            position -> {
                assertEquals(POSITION, position);
                return FOREST_CONTEXT;
            },
            biomeId -> {
                assertEquals(FOREST, biomeId);
                return policy;
            },
            currentSeason::get
        );

        assertEquals(TreeGrowthDecision.CANCEL_GROWTH, service.decideNaturalGrowth(POSITION));
        currentSeason.set(SUMMER);
        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, service.decideNaturalGrowth(POSITION));
    }

    @Test
    void preservesVanillaGrowthForAnUnsupportedBiome() {
        TreeGrowthService service = new TreeGrowthService(
            position -> { throw new UnsupportedBiomeException("Unsupported biome"); },
            biomeId -> { throw new AssertionError("Unexpected policy lookup"); },
            () -> { throw new AssertionError("Unexpected season query"); }
        );

        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, service.decideNaturalGrowth(POSITION));
    }

    @Test
    void preservesVanillaGrowthWhenTheBiomeHasNoTreeGrowthPolicy() {
        TreeGrowthService service = new TreeGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> { throw new UnsupportedTreeGrowthPolicyException("Missing policy"); },
            () -> { throw new AssertionError("Unexpected season query"); }
        );

        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, service.decideNaturalGrowth(POSITION));
    }

    @Test
    void propagatesUnexpectedFailures() {
        IllegalStateException failure = new IllegalStateException("Season unavailable");
        TreeGrowthService service = new TreeGrowthService(
            position -> FOREST_CONTEXT,
            biomeId -> new TreeGrowthPolicy(new TreeGrowthChance(0.8), () -> 0.6),
            () -> { throw failure; }
        );

        assertSame(failure, assertThrows(
            IllegalStateException.class,
            () -> service.decideNaturalGrowth(POSITION)
        ));
    }
}
