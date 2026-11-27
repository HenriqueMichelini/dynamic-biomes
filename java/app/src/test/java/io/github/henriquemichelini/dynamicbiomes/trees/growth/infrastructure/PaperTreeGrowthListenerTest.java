package io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.application.TreeGrowthService;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicy;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.world.StructureGrowEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaperTreeGrowthListenerTest {
    private static final UUID WORLD_ID = UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e");
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(WORLD_ID),
        -11,
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
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");

    @Test
    void cancelsNaturalGrowthAtTheOriginalSaplingLocationWhenPolicyDeniesIt() {
        StructureGrowEvent event = eventFor(TreeType.TREE, false, null);

        listenerWithChance(0.0).onStructureGrow(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void preservesNaturalGrowthWhenPolicyAllowsIt() {
        StructureGrowEvent event = eventFor(TreeType.CHERRY, false, null);

        listenerWithChance(1.0).onStructureGrow(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void ignoresGrowthMarkedAsBoneMealEvenWithoutAnAssociatedPlayer() {
        StructureGrowEvent event = eventFor(TreeType.TREE, true, null);

        listenerThatMustNotEvaluateGrowth().onStructureGrow(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void ignoresGrowthAssociatedWithAPlayer() {
        Player player = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] { Player.class },
            (proxy, method, arguments) -> { throw new AssertionError(method); }
        );
        StructureGrowEvent event = eventFor(TreeType.TREE, false, player);

        listenerThatMustNotEvaluateGrowth().onStructureGrow(event);

        assertFalse(event.isCancelled());
    }

    @ParameterizedTest
    @EnumSource(value = TreeType.class, names = {
        "AZALEA",
        "BROWN_MUSHROOM",
        "CHORUS_PLANT",
        "CRIMSON_FUNGUS",
        "JUNGLE_BUSH",
        "RED_MUSHROOM",
        "WARPED_FUNGUS"
    })
    void ignoresUnsupportedStructures(TreeType treeType) {
        StructureGrowEvent event = eventFor(treeType, false, null);

        listenerThatMustNotEvaluateGrowth().onStructureGrow(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void skipsGrowthAlreadyCancelledByAnotherListener() {
        StructureGrowEvent event = eventFor(TreeType.TREE, false, null);
        event.setCancelled(true);

        listenerThatMustNotEvaluateGrowth().onStructureGrow(event);

        assertTrue(event.isCancelled());
    }

    private static PaperTreeGrowthListener listenerWithChance(double chance) {
        return new PaperTreeGrowthListener(new TreeGrowthService(
            position -> {
                assertEquals(POSITION, position);
                return FOREST_CONTEXT;
            },
            biomeId -> new TreeGrowthPolicy(new TreeGrowthChance(chance), () -> 0.5),
            () -> SPRING
        ));
    }

    private static PaperTreeGrowthListener listenerThatMustNotEvaluateGrowth() {
        return new PaperTreeGrowthListener(new TreeGrowthService(
            position -> { throw new AssertionError("Unexpected biome resolution"); },
            biomeId -> { throw new AssertionError("Unexpected policy lookup"); },
            () -> { throw new AssertionError("Unexpected season query"); }
        ));
    }

    private static StructureGrowEvent eventFor(TreeType treeType, boolean boneMeal, Player player) {
        World world = (World) Proxy.newProxyInstance(
            World.class.getClassLoader(),
            new Class<?>[] { World.class },
            (proxy, method, arguments) -> {
                if (method.getName().equals("getUID")) {
                    return WORLD_ID;
                }
                throw new AssertionError(method);
            }
        );
        Location location = new Location(world, -10.5, 64, 20.5);
        return new StructureGrowEvent(location, treeType, boneMeal, player, List.of());
    }
}
