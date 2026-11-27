package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaperOreRemovalListenerTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")), 10, 64, -20
    );
    private static final BlockPosition NEARBY = new BlockPosition(POSITION.world(), 11, 64, -20);

    @TempDir
    Path temporaryDirectory;

    private final List<Runnable> nextTick = new ArrayList<>();
    private final AtomicReference<Material> material = new AtomicReference<>(Material.IRON_ORE);
    private Path originFile;
    private OreOriginTrackingService origins;
    private PaperOreRemovalListener listener;

    @BeforeEach
    void setUp() {
        originFile = temporaryDirectory.resolve("ore-origins.yml");
        origins = new OreOriginTrackingService(new YamlOreOriginRepository(originFile));
        origins.recordPlayerPlacedOre(POSITION);
        listener = new PaperOreRemovalListener(origins, nextTick::add);
    }

    @Test
    void successfulBreakWithoutDropsKeepsOriginUntilTheBlockIsGone() {
        BlockBreakEvent event = breakEvent();
        event.setDropItems(false);

        listener.onBlockBreak(event);
        assertTrue(origins.originAt(POSITION).isPresent());
        material.set(Material.AIR);
        nextTick.getFirst().run();

        assertTrue(origins.originAt(POSITION).isEmpty());
        assertTrue(reopenedOrigins().originAt(POSITION).isEmpty());
        assertFalse(event.isDropItems());
    }

    @Test
    void keepsOriginWhenBreakIsCancelledBeforeCleanup() {
        BlockBreakEvent event = breakEvent();

        listener.onBlockBreak(event);
        event.setCancelled(true);
        material.set(Material.AIR);
        nextTick.getFirst().run();

        assertTrue(origins.originAt(POSITION).isPresent());
    }

    @Test
    void keepsOriginWhenOreIsStillPresentAtCleanup() {
        listener.onBlockBreak(breakEvent());

        nextTick.getFirst().run();

        assertTrue(origins.originAt(POSITION).isPresent());
    }

    @Test
    void blockExplosionClearsDestroyedOresBeforeAnyReplacement() {
        origins.recordPlayerPlacedOre(NEARBY);
        Block ore = block(POSITION, material::get);
        Block otherOre = block(NEARBY, () -> Material.DEEPSLATE_DIAMOND_ORE);
        Block source = block(new BlockPosition(POSITION.world(), 12, 64, -20), () -> Material.AIR);
        BlockExplodeEvent event = new BlockExplodeEvent(
            source, proxy(BlockState.class, Map.of()), List.of(ore, otherOre), 0.0f, ExplosionResult.DESTROY
        );

        listener.onBlockExplode(event);
        material.set(Material.AIR);
        material.set(Material.IRON_ORE);

        assertTrue(origins.isEligibleForBiomeMultiplier(POSITION));
        assertTrue(reopenedOrigins().originAt(POSITION).isEmpty());
        assertTrue(reopenedOrigins().originAt(NEARBY).isEmpty());
        assertEquals(List.of(ore, otherOre).size(), event.blockList().size());
    }

    private OreOriginTrackingService reopenedOrigins() {
        return new OreOriginTrackingService(new YamlOreOriginRepository(originFile));
    }

    private BlockBreakEvent breakEvent() {
        return new BlockBreakEvent(block(POSITION, material::get), proxy(Player.class, Map.of()));
    }

    private static Block block(BlockPosition position, Supplier<Material> material) {
        World world = proxy(World.class, Map.of("getUID", position.world().id()));
        return proxy(Block.class, Map.of(
            "getType", material,
            "getWorld", world,
            "getX", position.x(),
            "getY", position.y(),
            "getZ", position.z()
        ));
    }

    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> {
                Object value = returnValues.get(method.getName());
                return value instanceof Supplier<?> supplier ? supplier.get() : value;
            }
        ));
    }
}
