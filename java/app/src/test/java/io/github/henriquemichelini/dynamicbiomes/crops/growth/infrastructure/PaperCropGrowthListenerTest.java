package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockGrowEvent;
import org.junit.jupiter.api.Test;

class PaperCropGrowthListenerTest {
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
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final BiomeContext FOREST_CONTEXT = new BiomeContext(
        FOREST,
        new BiomeProfile(
            FOREST,
            new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
            new Fertility(0.7)
        )
    );
    private BlockPosition resolvedPosition;
    private CropKind requestedCropKind;

    @Test
    void leavesWheatGrowthUncancelledWhenServiceAllowsGrowth() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.WHEAT);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(CropKind.WHEAT, requestedCropKind);
        assertEquals(1, proposedAge(event));
    }

    @Test
    void cancelsWheatGrowthWhenServiceCancelsGrowth() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(0.0, () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.WHEAT);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(CropKind.WHEAT, requestedCropKind);
    }

    @Test
    void delegatesCarrotGrowthWithCarrotCropKind() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.CARROTS);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(CropKind.CARROTS, requestedCropKind);
    }

    @Test
    void delegatesPotatoGrowthWithPotatoCropKind() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.POTATOES);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(CropKind.POTATOES, requestedCropKind);
    }

    @Test
    void delegatesBeetrootGrowthWithBeetrootCropKind() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            })
        );
        BlockGrowEvent event = eventFor(Material.BEETROOTS);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(CropKind.BEETROOT, requestedCropKind);
    }

    @Test
    void ignoresUnsupportedCropGrowthWithoutCallingService() {
        PaperCropGrowthListener listener = listenerWithResolver(position -> {
            throw new AssertionError("Service must not resolve biome for unsupported crop growth");
        });
        BlockGrowEvent event = eventFor(Material.NETHER_WART);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void ignoresAlreadyCancelledGrowthWithoutCallingService() {
        PaperCropGrowthListener listener = listenerWithResolver(position -> {
            throw new AssertionError("Service must not resolve biome for cancelled event");
        });
        BlockGrowEvent event = eventFor(Material.WHEAT);
        event.setCancelled(true);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void slowsAOneStageAdvanceWhenTheSpeedRollFails() {
        PaperCropGrowthListener listener = listenerWithPolicy(policy(1.0, () -> 0.5), 0.5);
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 3, 7);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
        assertEquals(3, proposedAge(event));
    }

    @Test
    void keepsTheVanillaAdvanceWhenAFractionalSpeedRollSucceeds() {
        PaperCropGrowthListener listener = listenerWithPolicy(policy(1.0, () -> 0.49), 0.5);
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 3, 7);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(3, proposedAge(event));
    }

    @Test
    void acceleratesAllowedGrowthThroughTheEventSnapshot() {
        AtomicInteger rollCount = new AtomicInteger();
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(0.8, () -> rollCount.getAndIncrement() == 0 ? 0.79 : 0.49),
            2.5
        );
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 3, 7);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(5, proposedAge(event));
        assertEquals(2, ((Ageable) event.getBlock().getBlockData()).getAge());
        assertEquals(2, rollCount.get());
    }

    @Test
    void capsAnArbitrarilyLargeSpeedFactorAtMaturityWithoutRolling() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Whole stages do not need a roll");
            }),
            Double.MAX_VALUE
        );
        BlockGrowEvent event = eventFor(Material.BEETROOTS, 1, 2, 3);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(3, proposedAge(event));
    }

    @Test
    void preservesTheProposedSnapshotWhenTheChanceGateRejectsGrowth() {
        PaperCropGrowthListener listener = listenerWithPolicy(policy(0.5, () -> 0.51), 2.5);
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 3, 7);

        listener.onBlockGrow(event);

        assertTrue(event.isCancelled());
        assertEquals(3, proposedAge(event));
    }

    @Test
    void keepsExistingChanceBehaviorForAMultipleStageAdvance() {
        PaperCropGrowthListener listener = listenerWithPolicy(
            policy(1.0, () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }),
            0.0
        );
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 5, 7);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(5, proposedAge(event));
    }

    @Test
    void leavesTheVanillaStageWhenThereIsNoSupportedPolicy() {
        PaperCropGrowthListener listener = listenerWithResolver(
            position -> FOREST_CONTEXT,
            (biomeId, cropKind) -> {
                throw new UnsupportedCropGrowthPolicyException("Missing crop policy");
            },
            0.0
        );
        BlockGrowEvent event = eventFor(Material.WHEAT, 2, 3, 7);

        listener.onBlockGrow(event);

        assertFalse(event.isCancelled());
        assertEquals(3, proposedAge(event));
    }

    @Test
    void propagatesServiceFailures() {
        PaperCropGrowthListener listener = listenerWithResolver(position -> {
            throw new IllegalStateException("Service failure");
        });
        BlockGrowEvent event = eventFor(Material.WHEAT);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> listener.onBlockGrow(event)
        );

        assertEquals("Service failure", exception.getMessage());
    }

    private PaperCropGrowthListener listenerWithPolicy(CropGrowthPolicy policy) {
        return listenerWithPolicy(policy, 1.0);
    }

    private PaperCropGrowthListener listenerWithPolicy(CropGrowthPolicy policy, double speedFactor) {
        return listenerWithResolver(
            position -> {
                resolvedPosition = position;
                return FOREST_CONTEXT;
            },
            (biomeId, cropKind) -> {
                requestedCropKind = cropKind;
                return policy;
            },
            speedFactor
        );
    }

    private PaperCropGrowthListener listenerWithResolver(BiomeResolver biomeResolver) {
        return listenerWithResolver(
            biomeResolver,
            (biomeId, cropKind) -> {
                throw new AssertionError("Policy lookup should not be reached");
            },
            1.0
        );
    }

    private static PaperCropGrowthListener listenerWithResolver(
        BiomeResolver biomeResolver,
        CropGrowthPolicyProvider policyProvider,
        double speedFactor
    ) {
        return new PaperCropGrowthListener(
            new CropGrowthService(
                biomeResolver,
                policyProvider,
                () -> SPRING,
                (position, cropKind) -> new CropPerformanceResult(
                    OptionalDouble.empty(),
                    speedFactor,
                    1.0,
                    1.0
                )
            )
        );
    }

    private static CropGrowthPolicy policy(
        double chance,
        CropGrowthVariation variation
    ) {
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            variation::nextUnitValue
        );
    }

    @FunctionalInterface
    private interface CropGrowthVariation {
        double nextUnitValue();
    }

    private static BlockGrowEvent eventFor(Material material) {
        return eventFor(material, 0, 1, 7);
    }

    private static BlockGrowEvent eventFor(Material material, int age, int proposedAge, int maximumAge) {
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", material,
                "getBlockData", ageable(material, age, maximumAge),
                "getWorld", world(),
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z()
            )
        );
        AtomicReference<BlockData> proposedData = new AtomicReference<>(ageable(material, proposedAge, maximumAge));
        BlockState newState = (BlockState) Proxy.newProxyInstance(
            BlockState.class.getClassLoader(),
            new Class<?>[] { BlockState.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getBlockData" -> proposedData.get();
                case "getType" -> material;
                case "setBlockData" -> {
                    proposedData.set((BlockData) arguments[0]);
                    yield null;
                }
                default -> throw new AssertionError("Unexpected snapshot operation: " + method.getName());
            }
        );
        return new BlockGrowEvent(block, newState);
    }

    private static int proposedAge(BlockGrowEvent event) {
        return ((Ageable) event.getNewState().getBlockData()).getAge();
    }

    private static Ageable ageable(Material material, int initialAge, int maximumAge) {
        int[] age = { initialAge };
        return (Ageable) Proxy.newProxyInstance(
            Ageable.class.getClassLoader(),
            new Class<?>[] { Ageable.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getMaterial" -> material;
                case "getAge" -> age[0];
                case "getMaximumAge" -> maximumAge;
                case "setAge" -> {
                    int newAge = (int) arguments[0];
                    assertTrue(newAge >= 0 && newAge <= maximumAge);
                    age[0] = newAge;
                    yield null;
                }
                default -> throw new AssertionError("Unexpected crop data operation: " + method.getName());
            }
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> {
                if (!returnValues.containsKey(method.getName())) {
                    throw new AssertionError("Unexpected world operation: " + method.getName());
                }
                return returnValues.get(method.getName());
            }
        );
    }

    private static World world() {
        return proxy(World.class, Map.of("getUID", WORLD_ID));
    }
}
