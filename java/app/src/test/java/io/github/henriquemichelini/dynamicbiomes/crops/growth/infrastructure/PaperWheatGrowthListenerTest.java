package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.WheatGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockGrowEvent;
import org.junit.jupiter.api.Test;

class PaperWheatGrowthListenerTest {
    private static final UUID WORLD_ID = UUID.fromString(
        "fdab89dd-8aac-4be0-9c26-8752ae6ce85e"
    );
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(WORLD_ID),
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
    private BlockPosition resolvedPosition;

    @Test
    void leavesWheatGrowthUncancelledWhenServiceAllowsGrowth() {
        PaperWheatGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.WHEAT);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
    }

    @Test
    void cancelsWheatGrowthWhenServiceCancelsGrowth() {
        PaperWheatGrowthListener listener = listenerWithPolicy(
            policy(0.0, () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.WHEAT);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
    }

    @Test
    void ignoresNonWheatGrowthWithoutChangingCancellationState() {
        PaperWheatGrowthListener listener = listenerWithResolver(position -> {
            throw new AssertionError("Service must not resolve biome for non-wheat growth");
        });
        BlockGrowEvent event = eventFor(Material.CARROTS);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void ignoresAlreadyCancelledGrowthWithoutCallingService() {
        PaperWheatGrowthListener listener = listenerWithResolver(position -> {
            throw new AssertionError("Service must not resolve biome for cancelled event");
        });
        BlockGrowEvent event = eventFor(Material.WHEAT);
        event.setCancelled(true);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void propagatesServiceFailures() {
        PaperWheatGrowthListener listener = listenerWithResolver(position -> {
            throw new IllegalStateException("Service failure");
        });
        BlockGrowEvent event = eventFor(Material.WHEAT);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> listener.onBlockGrow(event)
        );

        assertEquals("Service failure", exception.getMessage());
    }

    private PaperWheatGrowthListener listenerWithPolicy(WheatGrowthChancePolicy policy) {
        return listenerWithResolver(
            position -> {
                resolvedPosition = position;
                return FOREST_CONTEXT;
            },
            biomeId -> policy
        );
    }

    private PaperWheatGrowthListener listenerWithResolver(BiomeResolver biomeResolver) {
        return listenerWithResolver(
            biomeResolver,
            biomeId -> {
                throw new AssertionError("Policy lookup should not be reached");
            }
        );
    }

    private static PaperWheatGrowthListener listenerWithResolver(
        BiomeResolver biomeResolver,
        WheatGrowthChancePolicyProvider policyProvider
    ) {
        return new PaperWheatGrowthListener(
            new WheatGrowthService(biomeResolver, policyProvider)
        );
    }

    private static WheatGrowthChancePolicy policy(
        double chance,
        WheatGrowthVariation variation
    ) {
        return new WheatGrowthChancePolicy(
            new WheatGrowthChance(chance),
            variation::nextUnitValue
        );
    }

    @FunctionalInterface
    private interface WheatGrowthVariation {
        double nextUnitValue();
    }

    private static BlockGrowEvent eventFor(Material material) {
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", material,
                "getWorld", world(),
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z()
            )
        );
        BlockState newState = proxy(BlockState.class, Map.of());
        return new BlockGrowEvent(block, newState);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> returnValues.get(method.getName())
        );
    }

    private static World world() {
        return proxy(World.class, Map.of("getUID", WORLD_ID));
    }
}
