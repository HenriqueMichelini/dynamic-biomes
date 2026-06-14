package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.EcologicalPressure;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.MineralRichness;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropEnvironmentQueryService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private BlockPosition resolvedPosition;

    @Test
    void invokesBiomeAwareDropsBeforeClearingTrackedOrigin() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        PaperOreBreakListener listener = listener(repository);

        listener.onBlockBreak(eventFor(Material.IRON_ORE, world()));

        assertEquals(POSITION, resolvedPosition);
        assertEquals(POSITION, repository.removedPosition);
    }

    @Test
    void ignoresNonOreBreak() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        PaperOreBreakListener listener = listener(repository);

        listener.onBlockBreak(eventFor(Material.STONE, world()));

        assertNull(resolvedPosition);
        assertNull(repository.removedPosition);
    }

    @Test
    void checksPlayerPlacedOriginBeforeClearingIt() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED));
        PaperOreBreakListener listener = listener(repository);

        listener.onBlockBreak(eventFor(Material.IRON_ORE, world()));

        assertNull(resolvedPosition);
        assertEquals(POSITION, repository.removedPosition);
    }

    @Test
    void silkTouchBypassesMultiplierAndPreservesVanillaDrops() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.NATURAL));
        PaperOreBreakListener listener = listener(repository);

        List<ItemStack> silkTouchDrops = List.of(new FakeItemStack(Material.IRON_ORE));
        BlockBreakEvent event = eventFor(Material.IRON_ORE, world(), silkTouchDrops);
        listener.onBlockBreak(event);

        assertNull(resolvedPosition);
        assertEquals(POSITION, repository.removedPosition);
    }

    @Test
    void nonSilkTouchStillCallsOreDropService() {
        RecordingOreOriginRepository repository = new RecordingOreOriginRepository();
        repository.save(new OreOrigin(POSITION, OreOriginType.NATURAL));
        PaperOreBreakListener listener = listener(repository);

        List<ItemStack> normalDrops = List.of(new FakeItemStack(Material.RAW_IRON));
        BlockBreakEvent event = eventFor(Material.IRON_ORE, world(), normalDrops);
        listener.onBlockBreak(event);

        assertEquals(POSITION, resolvedPosition);
        assertEquals(POSITION, repository.removedPosition);
    }

    private PaperOreBreakListener listener(OreOriginRepository repository) {
        OreOriginTrackingService originTracking = new OreOriginTrackingService(repository);
        BiomeContext forestContext = new BiomeContext(
            FOREST,
            new BiomeProfile(
                FOREST,
                new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
                new Fertility(0.7),
                new MineralRichness(0.3),
                new EcologicalPressure(0.2)
            )
        );
        SeasonProfile springProfile = new SeasonProfile(
            new SeasonId("minecraft:spring"),
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(0.0),
                new SeasonalAdjustment(0.1)
            )
        );
        OreDropEnvironmentQueryService environmentQuery = new OreDropEnvironmentQueryService(
            position -> {
                resolvedPosition = position;
                return forestContext;
            },
            () -> springProfile.seasonId(),
            seasonId -> springProfile
        );
        OreDropService dropService = new OreDropService(
            originTracking,
            environmentQuery,
            biomeId -> new OreDropPolicy(
                biomeId,
                Map.of(IRON_ORE, new OreDropMultiplierRange(2.0, 2.0))
            ),
            new OreDropMultiplierCalculator(() -> 0.5),
            new OreDropQuantityCalculator(() -> 0.5)
        );
        return new PaperOreBreakListener(dropService, originTracking);
    }

    private static BlockBreakEvent eventFor(Material material, World world) {
        return eventFor(material, world, List.of());
    }

    private static BlockBreakEvent eventFor(
        Material material,
        World world,
        List<ItemStack> drops
    ) {
        PlayerInventory inventory = proxy(
            PlayerInventory.class,
            Map.of()
        );
        Player player = proxy(Player.class, Map.of("getInventory", inventory));
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", material,
                "getWorld", world,
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z(),
                "getDrops", drops
            )
        );
        return new BlockBreakEvent(block, player);
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

    private static final class RecordingOreOriginRepository
        implements OreOriginRepository {
        private OreOrigin origin;
        private BlockPosition removedPosition;

        @Override
        public void save(OreOrigin origin) {
            this.origin = origin;
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            return Optional.ofNullable(origin);
        }

        @Override
        public void removeByPosition(BlockPosition position) {
            origin = null;
            removedPosition = position;
        }
    }

    private static final class FakeItemStack extends ItemStack {
        private final Material type;
        private int amount = 1;

        FakeItemStack(Material type) {
            super();
            this.type = type;
        }

        @Override
        public Material getType() {
            return type;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            FakeItemStack copy = new FakeItemStack(type);
            copy.amount = this.amount;
            return copy;
        }
    }
}
