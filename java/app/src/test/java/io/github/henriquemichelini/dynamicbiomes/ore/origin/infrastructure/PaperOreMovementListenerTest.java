package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.junit.jupiter.api.Test;

class PaperOreMovementListenerTest {

    private static final UUID WORLD_ID = UUID.fromString(
        "fdab89dd-8aac-4be0-9c26-8752ae6ce85e"
    );
    private static final BlockPosition SOURCE = new BlockPosition(
        new WorldReference(WORLD_ID),
        10,
        64,
        -20
    );

    @Test
    void pistonExtendMovesTrackedPlayerPlacedOre() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();

        repository.save(new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED));

        PaperOreMovementListener listener = listener(repository);

        BlockPosition destination = new BlockPosition(
            SOURCE.world(),
            SOURCE.x(),
            SOURCE.y(),
            SOURCE.z() - 1
        );
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.IRON_ORE, SOURCE)),
            BlockFace.NORTH
        );
        listener.onPistonExtend(event);

        assertTrue(repository.findByPosition(SOURCE).isEmpty());
        assertEquals(
            new OreOrigin(destination, OreOriginType.PLAYER_PLACED),
            repository.findByPosition(destination).orElseThrow()
        );
    }

    @Test
    void pistonExtendMovesTrackedConfiguredNonIronOre() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED));
        PaperOreMovementListener listener = listener(repository);

        BlockPosition destination = new BlockPosition(
            SOURCE.world(),
            SOURCE.x(),
            SOURCE.y(),
            SOURCE.z() - 1
        );
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.DIAMOND_ORE, SOURCE)),
            BlockFace.NORTH
        );
        listener.onPistonExtend(event);

        assertTrue(repository.findByPosition(SOURCE).isEmpty());
        assertEquals(
            new OreOrigin(destination, OreOriginType.PLAYER_PLACED),
            repository.findByPosition(destination).orElseThrow()
        );
    }

    @Test
    void pistonExtendIgnoresUntrackedOre() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        PaperOreMovementListener listener = listener(repository);

        BlockPosition destination = new BlockPosition(
            SOURCE.world(),
            SOURCE.x(),
            SOURCE.y(),
            SOURCE.z() - 1
        );
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.IRON_ORE, SOURCE)),
            BlockFace.NORTH
        );
        listener.onPistonExtend(event);

        assertTrue(repository.findByPosition(SOURCE).isEmpty());
        assertTrue(repository.findByPosition(destination).isEmpty());
    }

    @Test
    void pistonPushingAdjacentOresPreservesTheOriginOfEachBlock() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        BlockPosition next = new BlockPosition(SOURCE.world(), SOURCE.x(), SOURCE.y(), SOURCE.z() - 1);
        BlockPosition last = new BlockPosition(SOURCE.world(), SOURCE.x(), SOURCE.y(), SOURCE.z() - 2);
        repository.save(new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED));
        repository.save(new OreOrigin(next, OreOriginType.NATURAL));
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.IRON_ORE, SOURCE), movedBlock(Material.DIAMOND_ORE, next)),
            BlockFace.NORTH
        );

        listener(repository).onPistonExtend(event);

        assertTrue(repository.findByPosition(SOURCE).isEmpty());
        assertEquals(Optional.of(new OreOrigin(next, OreOriginType.PLAYER_PLACED)), repository.findByPosition(next));
        assertEquals(Optional.of(new OreOrigin(last, OreOriginType.NATURAL)), repository.findByPosition(last));
    }

    @Test
    void pistonExtendIgnoresNonOreBlocks() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED));
        PaperOreMovementListener listener = listener(repository);

        BlockPosition destination = new BlockPosition(
            SOURCE.world(),
            SOURCE.x(),
            SOURCE.y(),
            SOURCE.z() - 1
        );
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.STONE, SOURCE)),
            BlockFace.NORTH
        );
        listener.onPistonExtend(event);

        assertEquals(
            new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED),
            repository.findByPosition(SOURCE).orElseThrow()
        );
        assertTrue(repository.findByPosition(destination).isEmpty());
    }

    @Test
    void pistonRetractUsesTheMovementDirectionReportedByPaper() {
        InMemoryOreOriginRepository repository =
            new InMemoryOreOriginRepository();
        repository.save(new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED));
        PaperOreMovementListener listener = listener(repository);

        BlockPosition destination = new BlockPosition(
            SOURCE.world(),
            SOURCE.x(),
            SOURCE.y(),
            SOURCE.z() + 1
        );
        BlockPistonRetractEvent event = new BlockPistonRetractEvent(
            pistonBlock(),
            List.of(movedBlock(Material.IRON_ORE, SOURCE)),
            BlockFace.SOUTH
        );
        listener.onPistonRetract(event);

        assertTrue(repository.findByPosition(SOURCE).isEmpty());
        assertEquals(
            new OreOrigin(destination, OreOriginType.PLAYER_PLACED),
            repository.findByPosition(destination).orElseThrow()
        );
    }

    @Test
    void cancelsMovementWhenTheMovedOriginsCannotBePersisted() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        OreOrigin origin = new OreOrigin(SOURCE, OreOriginType.PLAYER_PLACED);
        repository.save(origin);
        repository.failWrites = true;
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(
            pistonBlock(),
            List.of(movedBlock(Material.IRON_ORE, SOURCE)),
            BlockFace.NORTH
        );

        assertThrows(IllegalStateException.class, () -> listener(repository).onPistonExtend(event));

        assertTrue(event.isCancelled());
        assertEquals(Optional.of(origin), repository.findByPosition(SOURCE));
    }

    private static PaperOreMovementListener listener(
        OreOriginRepository repository
    ) {
        return new PaperOreMovementListener(
            new OreOriginTrackingService(repository)
        );
    }

    private static Block pistonBlock() {
        return proxy(Block.class, Map.of());
    }

    private static Block movedBlock(Material material, BlockPosition position) {
        World world = proxy(World.class, Map.of("getUID", WORLD_ID));
        return proxy(
            Block.class,
            Map.of(
                "getType",
                material,
                "getWorld",
                world,
                "getX",
                position.x(),
                "getY",
                position.y(),
                "getZ",
                position.z()
            )
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(
        Class<T> type,
        Map<String, Object> returnValues
    ) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> returnValues.get(method.getName())
        );
    }

    private static final class InMemoryOreOriginRepository
        implements OreOriginRepository
    {

        private final Map<BlockPosition, OreOrigin> origins = new HashMap<>();
        private boolean failWrites;

        @Override
        public void save(OreOrigin origin) {
            origins.put(origin.position(), origin);
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            return Optional.ofNullable(origins.get(position));
        }

        @Override
        public void removeByPosition(BlockPosition position) {
            origins.remove(position);
        }

        @Override
        public void applyChanges(
            Collection<BlockPosition> removedPositions,
            Collection<OreOrigin> savedOrigins
        ) {
            if (failWrites) throw new IllegalStateException("Origin storage unavailable");
            removedPositions.forEach(this::removeByPosition);
            savedOrigins.forEach(this::save);
        }
    }
}
