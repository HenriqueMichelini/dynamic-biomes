package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.application.CropYieldService;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
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
    private final RecordingActionBarNotifier notifier = new RecordingActionBarNotifier();
    private BlockPosition resolvedPosition;

    @Test
    void ignoresUnsupportedMaterials() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(Material.STONE, true, List.of());

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(List.of(), event.getItems());
        assertEquals(List.of(), notifier.notifications);
        assertNull(resolvedPosition);
    }

    @Test
    void ignoresImmatureCrops() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            false,
            List.of(new FakeItemStack(Material.WHEAT, 1))
        );

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(List.of(1), droppedAmounts(event));
        assertEquals(List.of(), notifier.notifications);
        assertNull(resolvedPosition);
    }

    @Test
    void ignoresZeroVanillaProduceQuantity() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(new FakeItemStack(Material.WHEAT_SEEDS, 2))
        );

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(List.of(2), droppedAmounts(event));
        assertEquals(List.of(), notifier.notifications);
        assertNull(resolvedPosition);
    }

    @Test
    void doublesTheAmountPaperRolledAndPreservesTheEntities() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(
                new FakeItemStack(Material.WHEAT, 3),
                new FakeItemStack(Material.WHEAT_SEEDS, 2)
            )
        );
        Item originalProduce = event.getItems().getFirst();
        Item originalSeeds = event.getItems().getLast();

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(POSITION, resolvedPosition);
        assertSame(originalProduce, event.getItems().getFirst());
        assertSame(originalSeeds, event.getItems().getLast());
        assertEquals(List.of(6, 2), droppedAmounts(event));
        assertEquals(
            List.of(
                new ActionBarNotification(
                    "+3 wheat extra!",
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
        BlockDropItemEvent event = eventFor(
            Material.BEETROOTS,
            true,
            List.of(
                new FakeItemStack(Material.BEETROOT, 1),
                new FakeItemStack(Material.BEETROOT_SEEDS, 3)
            )
        );
        Item originalSeeds = event.getItems().getLast();

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(POSITION, resolvedPosition);
        assertSame(originalSeeds, event.getItems().getFirst());
        assertEquals(List.of(3), droppedAmounts(event));
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
    void keepsExtraStackWithinPaperDropEventUntilItCompletes() {
        PaperCropHarvestListener listener = listener(3.0);
        BlockDropItemEvent event = eventFor(
            Material.BEETROOTS,
            true,
            List.of(new FakeItemStack(Material.BEETROOT, 32))
        );
        Item original = event.getItems().getFirst();
        Vector velocity = new Vector(0.03, 0.10, 0.02);
        original.setVelocity(velocity);
        original.setPickupDelay(17);

        listener.onBlockDrop(event);

        assertSame(original, event.getItems().getFirst());
        assertEquals(List.of(64, 32), droppedAmounts(event));
        assertEquals(velocity, event.getItems().getLast().getVelocity());
        assertEquals(17, event.getItems().getLast().getPickupDelay());
        assertEquals(List.of(), notifier.notifications);
    }

    @Test
    void reducesProduceAcrossMultipleEntitiesWithoutTouchingInterleavedDrops() {
        PaperCropHarvestListener listener = listener(0.25);
        BlockDropItemEvent event = eventFor(
            Material.BEETROOTS,
            true,
            List.of(
                new FakeItemStack(Material.BEETROOT, 4),
                new FakeItemStack(Material.POISONOUS_POTATO, 1),
                new FakeItemStack(Material.BEETROOT, 4)
            )
        );
        Item secondDrop = event.getItems().get(1);

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertSame(secondDrop, event.getItems().getLast());
        assertEquals(List.of(2, 1), droppedAmounts(event));
    }

    @Test
    void doesNotSendActionBarWhenAdjustedQuantityMatchesVanillaQuantity() {
        PaperCropHarvestListener listener = listener(1.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(
                new FakeItemStack(Material.WHEAT, 1),
                new FakeItemStack(Material.WHEAT_SEEDS, 2)
            )
        );

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(POSITION, resolvedPosition);
        assertEquals(List.of(1, 2), droppedAmounts(event));
        assertEquals(List.of(), notifier.notifications);
    }

    @Test
    void doesNotAdjustOrSendActionBarForAlreadyCancelledEvents() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(new FakeItemStack(Material.WHEAT, 1))
        );
        event.setCancelled(true);

        listener.onBlockDrop(event);
        listener.reportYieldDelta(event);

        assertEquals(List.of(1), droppedAmounts(event));
        assertEquals(List.of(), notifier.notifications);
        assertNull(resolvedPosition);
    }

    @Test
    void doesNotSendActionBarWhenAnotherHandlerCancelsTheDrops() {
        PaperCropHarvestListener listener = listener(2.0);
        BlockDropItemEvent event = eventFor(
            Material.WHEAT,
            true,
            List.of(new FakeItemStack(Material.WHEAT, 1))
        );

        listener.onBlockDrop(event);
        event.setCancelled(true);
        listener.reportYieldDelta(event);

        assertEquals(List.of(), notifier.notifications);
    }

    private PaperCropHarvestListener listener(double multiplier) {
        BiomeContext forestContext = new BiomeContext(
            FOREST,
            new BiomeProfile(
                FOREST,
                new ClimateProfile(new Humidity(0.4), new Temperature(0.8)),
                new Fertility(0.5)
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
            (position, cropKind) -> new CropPerformanceResult(
                OptionalDouble.empty(),
                1.0,
                1.0,
                1.0
            ),
            new CropYieldMultiplierCalculator(() -> 0.0),
            new CropYieldQuantityCalculator(() -> 0.0)
        );
        return new PaperCropHarvestListener(service, notifier);
    }

    private static BlockDropItemEvent eventFor(
        Material material,
        boolean mature,
        List<ItemStack> drops
    ) {
        World world = world();
        Player player = proxy(Player.class, Map.of());
        Block block = proxy(
            Block.class,
            Map.of(
                "getType", Material.AIR,
                "getWorld", world,
                "getX", POSITION.x(),
                "getY", POSITION.y(),
                "getZ", POSITION.z()
            )
        );
        BlockState harvestedState = proxy(
            BlockState.class,
            Map.of("getType", material, "getBlockData", ageable(mature))
        );
        Location location = new Location(world, POSITION.x(), POSITION.y(), POSITION.z());
        List<Item> capturedItems = new ArrayList<>();
        for (ItemStack drop : drops) {
            capturedItems.add(item(location, drop));
        }
        return new BlockDropItemEvent(block, harvestedState, player, capturedItems);
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

    private static <T> T proxy(Class<T> type, Map<String, Object> returnValues) {
        return proxy(type, (proxy, method, arguments) -> {
            if (!returnValues.containsKey(method.getName())) {
                throw new AssertionError("Unexpected " + type.getSimpleName() + " operation: " + method.getName());
            }
            return returnValues.get(method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            handler
        ));
    }

    private static World world() {
        return proxy(World.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getUID" -> WORLD_ID;
            case "createEntity" -> {
                assertEquals(Item.class, arguments[1]);
                yield item((Location) arguments[0], null);
            }
            default -> throw new AssertionError("Unexpected world operation: " + method.getName());
        });
    }

    private static Item item(Location location, ItemStack originalStack) {
        AtomicReference<ItemStack> stack = new AtomicReference<>(originalStack);
        AtomicReference<Vector> velocity = new AtomicReference<>(new Vector());
        AtomicInteger pickupDelay = new AtomicInteger();
        return proxy(Item.class, (proxy, method, arguments) -> switch (method.getName()) {
            case "getLocation" -> location;
            case "getWorld" -> location.getWorld();
            case "getItemStack" -> stack.get().clone();
            case "getVelocity" -> velocity.get().clone();
            case "getPickupDelay" -> pickupDelay.get();
            case "setItemStack" -> {
                stack.set((ItemStack) arguments[0]);
                yield null;
            }
            case "setVelocity" -> {
                velocity.set(((Vector) arguments[0]).clone());
                yield null;
            }
            case "setPickupDelay" -> {
                pickupDelay.set((int) arguments[0]);
                yield null;
            }
            default -> throw new AssertionError("Unexpected item operation: " + method.getName());
        });
    }

    private static List<Integer> droppedAmounts(BlockDropItemEvent event) {
        return event.getItems().stream()
            .map(Item::getItemStack)
            .map(ItemStack::getAmount)
            .toList();
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
