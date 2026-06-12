package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.junit.jupiter.api.Test;

class PaperOreBreakListenerTest {
    private static final UUID WORLD_ID = UUID.fromString(
        "fdab89dd-8aac-4be0-9c26-8752ae6ce85e"
    );
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(WORLD_ID),
        10,
        64,
        -20
    );

    @Test
    void clearsTrackedOriginForConfiguredOreBreak() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        PaperOreBreakListener listener = listener(repository);

        listener.onBlockBreak(eventFor(Material.IRON_ORE));

        assertEquals(POSITION, repository.removedPosition);
    }

    @Test
    void ignoresNonOreBreak() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        PaperOreBreakListener listener = listener(repository);

        listener.onBlockBreak(eventFor(Material.STONE));

        assertNull(repository.removedPosition);
    }

    private static PaperOreBreakListener listener(OreOriginRepository repository) {
        return new PaperOreBreakListener(new OreOriginTrackingService(repository));
    }

    private static BlockBreakEvent eventFor(Material material) {
        World world = proxy(
            World.class,
            Map.of("getUID", WORLD_ID)
        );
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", material,
                "getWorld", world,
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z()
            )
        );
        return new BlockBreakEvent(block, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> returnValues.get(method.getName())
        );
    }

    private static final class RecordingOreOriginRepository
        implements OreOriginRepository {
        private BlockPosition removedPosition;

        @Override
        public void save(OreOrigin origin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeByPosition(BlockPosition position) {
            removedPosition = position;
        }
    }
}
