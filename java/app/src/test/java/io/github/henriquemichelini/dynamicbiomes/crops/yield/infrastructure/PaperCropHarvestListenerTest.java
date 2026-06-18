package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.application.CropYieldService;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldBiomeFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldClimateFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEffectiveMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEnvironmentalFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class PaperCropHarvestListenerTest {
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
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private final List<ItemStack> droppedItems = new ArrayList<>();
    private final RecordingActionBarNotifier notifier = new RecordingActionBarNotifier();
    private BlockPosition resolvedPosition;

    @Test
    void ignoresUnsupportedMaterials() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockBreakEvent event = eventFor(Material.STONE, true, List.of());

        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
        assertEquals(List.of(), droppedItems);
        assertEquals(List.of(), notifier.notifications);
        assertEquals(null, resolvedPosition);
    }

    @Test
    void ignoresImmatureCrops() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockBreakEvent event = eventFor(
            Material.WHEAT,
            false,
            List.of(new FakeItemStack(Material.WHEAT, 1))
        );

        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
        assertEquals(List.of(), droppedItems);
        assertEquals(List.of(), notifier.notifications);
        assertEquals(null, resolvedPosition);
    }

    @Test
    void ignoresZeroVanillaProduceQuantity() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockBreakEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(new FakeItemStack(Material.WHEAT_SEEDS, 2))
        );

        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
        assertEquals(List.of(), droppedItems);
        assertEquals(List.of(), notifier.notifications);
        assertEquals(null, resolvedPosition);
    }

    @Test
    void adjustsMatureProduceDropsForPlayerBreaks() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockBreakEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(
                new FakeItemStack(Material.WHEAT, 1),
                new FakeItemStack(Material.WHEAT_SEEDS, 2)
            )
        );

        listener.onBlockBreak(event);

        assertFalse(event.isDropItems());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(List.of(Material.WHEAT_SEEDS, Material.WHEAT), droppedTypes());
        assertEquals(List.of(2, 2), droppedAmounts());
        assertEquals(
            List.of(
                new ActionBarNotification(
                    "+1 wheat extra!",
                    TextColor.color(0x90EE90),
                    CropYieldDeltaTone.POSITIVE
                )
            ),
            notifier.notifications
        );
    }

    @Test
    void preservesSeedsWhenProduceQuantityBecomesZero() {
        PaperCropHarvestListener listener = listener(0.0);
        BlockBreakEvent event = eventFor(
            Material.BEETROOTS,
            true,
            List.of(
                new FakeItemStack(Material.BEETROOT, 1),
                new FakeItemStack(Material.BEETROOT_SEEDS, 3)
            )
        );

        listener.onBlockBreak(event);

        assertFalse(event.isDropItems());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(List.of(Material.BEETROOT_SEEDS), droppedTypes());
        assertEquals(List.of(3), droppedAmounts());
        assertEquals(
            List.of(
                new ActionBarNotification(
                    "-1 beetroot",
                    NamedTextColor.RED,
                    CropYieldDeltaTone.NEGATIVE
                )
            ),
            notifier.notifications
        );
    }

    @Test
    void doesNotSendActionBarWhenAdjustedQuantityMatchesVanillaQuantity() {
        PaperCropHarvestListener listener = listener(1.0);
        BlockBreakEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(
                new FakeItemStack(Material.WHEAT, 1),
                new FakeItemStack(Material.WHEAT_SEEDS, 2)
            )
        );

        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
        assertEquals(POSITION, resolvedPosition);
        assertEquals(List.of(), droppedItems);
        assertEquals(List.of(), notifier.notifications);
    }

    @Test
    void doesNotSendActionBarForCancelledEvents() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockBreakEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(new FakeItemStack(Material.WHEAT, 1))
        );
        event.setCancelled(true);

        listener.onBlockBreak(event);

        assertTrue(event.isDropItems());
        assertEquals(List.of(), droppedItems);
        assertEquals(List.of(), notifier.notifications);
        assertEquals(null, resolvedPosition);
    }

    private PaperCropHarvestListener listener(double multiplier) {
        BiomeContext forestContext = new BiomeContext(
            FOREST,
            new BiomeProfile(
                FOREST,
                new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
                new Fertility(0.5),
                new MineralRichness(0.3),
                new EcologicalPressure(0.2)
            )
        );
        CropYieldService service = new CropYieldService(
            position -> {
                resolvedPosition = position;
                return forestContext;
            },
            biomeId -> new CropYieldPolicy(
                biomeId,
                Map.of(
                    CropKind.WHEAT,
                    new CropYieldCropRule(
                        new CropYieldMultiplierRange(multiplier, multiplier),
                        Map.of()
                    ),
                    CropKind.BEETROOT,
                    new CropYieldCropRule(
                        new CropYieldMultiplierRange(multiplier, multiplier),
                        Map.of()
                    )
                )
            ),
            () -> SUMMER,
            seasonId -> seasonProfile(seasonId),
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0),
            new CropYieldBiomeFactorCalculator(),
            new CropYieldClimateFactorCalculator(),
            new CropYieldEnvironmentalFactorCalculator(),
            new CropYieldEffectiveMultiplierCalculator()
        );
        return new PaperCropHarvestListener(
            service,
            (world, location, itemStack) -> droppedItems.add(itemStack.clone()),
            notifier
        );
    }

    private static SeasonProfile seasonProfile(SeasonId seasonId) {
        return new SeasonProfile(
            seasonId,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(0.0),
                new SeasonalAdjustment(0.0)
            )
        );
    }

    private BlockBreakEvent eventFor(
        Material material,
        boolean mature,
        List<ItemStack> drops
    ) {
        World world = world();
        PlayerInventory inventory = proxy(PlayerInventory.class, Map.of());
        Player player = proxy(Player.class, Map.of("getInventory", inventory));
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", material,
                "getWorld", world,
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z(),
                "getLocation", new Location(world, POSITION.x(), POSITION.y(), POSITION.z()),
                "getDrops", drops,
                "getBlockData", ageable(mature)
            )
        );
        return new BlockBreakEvent(block, player);
    }

    private static BlockData ageable(boolean mature) {
        return proxy(
            Ageable.class,
            Map.of(
                "getAge", mature ? 7 : 3,
                "getMaximumAge", 7
            )
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

    private static World world() {
        return proxy(World.class, Map.of("getUID", WORLD_ID));
    }

    private List<Material> droppedTypes() {
        return droppedItems.stream().map(ItemStack::getType).toList();
    }

    private List<Integer> droppedAmounts() {
        return droppedItems.stream().map(ItemStack::getAmount).toList();
    }

    private static final class RecordingActionBarNotifier
        implements CropYieldActionBarNotifier {
        private final List<ActionBarNotification> notifications = new ArrayList<>();

        @Override
        public void send(
            Player player,
            String message,
            TextColor color,
            CropYieldDeltaTone tone
        ) {
            notifications.add(new ActionBarNotification(message, color, tone));
        }
    }

    private record ActionBarNotification(
        String message,
        TextColor color,
        CropYieldDeltaTone tone
    ) {}

    private static final class FakeItemStack extends ItemStack {
        private final Material type;
        private int amount;

        FakeItemStack(Material type, int amount) {
            super();
            this.type = type;
            this.amount = amount;
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
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public ItemStack clone() {
            return new FakeItemStack(type, amount);
        }
    }
}
