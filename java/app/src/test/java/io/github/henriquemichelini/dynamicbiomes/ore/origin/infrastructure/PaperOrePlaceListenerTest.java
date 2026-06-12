package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

class PaperOrePlaceListenerTest {
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
    void recordsConfiguredOrePlacementAsPlayerPlaced() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        PaperOrePlaceListener listener = listener(repository);

        listener.onBlockPlace(eventFor(Material.IRON_ORE));

        assertEquals(
            new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED),
            repository.findByPosition(POSITION).orElseThrow()
        );
    }

    @Test
    void ignoresNonOrePlacement() {
        InMemoryOreOriginRepository repository = new InMemoryOreOriginRepository();
        PaperOrePlaceListener listener = listener(repository);

        listener.onBlockPlace(eventFor(Material.STONE));

        assertTrue(repository.findByPosition(POSITION).isEmpty());
    }

    private static PaperOrePlaceListener listener(OreOriginRepository repository) {
        return new PaperOrePlaceListener(new OreOriginTrackingService(repository));
    }

    private static BlockPlaceEvent eventFor(Material material) {
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
        return new BlockPlaceEvent(
            block,
            null,
            null,
            null,
            null,
            true,
            EquipmentSlot.HAND
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> returnValues.get(method.getName())
        );
    }

    private static final class InMemoryOreOriginRepository
        implements OreOriginRepository {
        private final Map<BlockPosition, OreOrigin> origins = new HashMap<>();

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
    }
}
