package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropOreRule;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class PaperOreBreakListenerTest {
    private static final UUID WORLD_ID = UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e");
    private static final BlockPosition POSITION = new BlockPosition(new WorldReference(WORLD_ID), 10, 64, -20);
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final OreKind DIAMOND_ORE = new OreKind("minecraft:diamond_ore");

    private final RecordingOriginRepository repository = new RecordingOriginRepository();
    private final List<Notification> notifications = new ArrayList<>();
    private BlockPosition resolvedPosition;

    @Test
    void adjustsOnlyActualOreDropsAndReportsFinalIncrease() {
        PaperOreBreakListener listener = listener(2.0);
        Item attachment = item(Material.TORCH, 1);
        Item oreDrop = item(Material.RAW_IRON, 3);
        BlockDropItemEvent event = dropEvent(Material.IRON_ORE, attachment, oreDrop);

        listener.onBlockDropItems(event);
        assertTrue(notifications.isEmpty());
        listener.onBlockDropItemsFinished(event);

        assertEquals(POSITION, resolvedPosition);
        assertEquals(List.of(attachment, oreDrop), event.getItems());
        assertEquals(1, attachment.getItemStack().getAmount());
        assertEquals(6, oreDrop.getItemStack().getAmount());
        assertEquals(
            List.of(new Notification("+3 iron ore extra!", TextColor.color(0x90EE90), OreDropDeltaTone.POSITIVE)),
            notifications
        );
    }

    @Test
    void keepsOverflowStacksInTheCancellableEvent() {
        PaperOreBreakListener listener = listener(20.0);
        Item capturedDrop = item(Material.DIAMOND, 7);
        capturedDrop.setPickupDelay(14);
        capturedDrop.setVelocity(new Vector(0.1, 0.2, -0.3));
        BlockDropItemEvent event = dropEvent(Material.DIAMOND_ORE, capturedDrop);

        listener.onBlockDropItems(event);

        assertEquals(
            List.of(64, 64, 12),
            event.getItems().stream().map(item -> item.getItemStack().getAmount()).toList()
        );
        for (Item overflow : event.getItems().subList(1, event.getItems().size())) {
            assertEquals(capturedDrop.getPickupDelay(), overflow.getPickupDelay());
            assertEquals(capturedDrop.getVelocity(), overflow.getVelocity());
        }

        event.setCancelled(true);
        listener.onBlockDropItemsFinished(event);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void reducesOreStacksWhilePreservingOtherDrops() {
        PaperOreBreakListener listener = listener(0.5);
        Item oreDrop = item(Material.RAW_IRON, 2);
        Item attachment = item(Material.TORCH, 1);
        BlockDropItemEvent event = dropEvent(Material.IRON_ORE, oreDrop, attachment);

        listener.onBlockDropItems(event);
        listener.onBlockDropItemsFinished(event);

        assertEquals(1, oreDrop.getItemStack().getAmount());
        assertSame(attachment, event.getItems().getLast());
        assertEquals(
            List.of(new Notification("-1 iron ore", NamedTextColor.RED, OreDropDeltaTone.NEGATIVE)),
            notifications
        );
    }

    @Test
    void zeroAdjustedYieldRemovesOnlyTheOreItems() {
        PaperOreBreakListener listener = listener(0.0);
        Item attachment = item(Material.TORCH, 1);
        BlockDropItemEvent event = dropEvent(
            Material.DIAMOND_ORE, item(Material.DIAMOND, 1), attachment, item(Material.DIAMOND, 3)
        );

        listener.onBlockDropItems(event);

        assertEquals(List.of(attachment), event.getItems());
    }

    @Test
    void leavesDropsAndFeedbackUnchangedWhenQuantityIsUnchanged() {
        PaperOreBreakListener listener = listener(1.0);
        Item oreDrop = item(Material.RAW_IRON, 2);
        ItemStack originalStack = oreDrop.getItemStack();
        BlockDropItemEvent event = dropEvent(Material.IRON_ORE, oreDrop);

        listener.onBlockDropItems(event);
        listener.onBlockDropItemsFinished(event);

        assertSame(originalStack, oreDrop.getItemStack());
        assertTrue(notifications.isEmpty());
    }

    @Test
    void unsupportedBiomeOrPolicyPreservesActualDrops() {
        BiomeResolver unsupportedBiome = position -> {
            throw new UnsupportedBiomeException("Missing biome profile");
        };
        OreDropPolicyProvider unsupportedPolicy = biomeId -> {
            throw new UnsupportedOreDropConfigurationException("Missing ore policy");
        };
        List<PaperOreBreakListener> listeners = List.of(
            listener(unsupportedBiome, fixedPolicy(2.0)),
            listener(this::resolveForest, unsupportedPolicy)
        );

        for (PaperOreBreakListener listener : listeners) {
            Item oreDrop = item(Material.RAW_IRON, 2);
            ItemStack originalStack = oreDrop.getItemStack();
            BlockDropItemEvent event = dropEvent(Material.IRON_ORE, oreDrop);

            listener.onBlockDropItems(event);
            listener.onBlockDropItemsFinished(event);

            assertSame(originalStack, oreDrop.getItemStack());
            assertTrue(notifications.isEmpty());
        }
    }

    @Test
    void ignoresEmptyDropsAndNonOreBreaks() {
        PaperOreBreakListener listener = listener(2.0);
        listener.onBlockDropItems(dropEvent(Material.IRON_ORE));
        listener.onBlockDropItems(dropEvent(Material.STONE, item(Material.COBBLESTONE, 1)));

        assertNull(resolvedPosition);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void leavesPlayerPlacedOriginAndDropsUnchanged() {
        repository.save(new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED));
        PaperOreBreakListener listener = listener(2.0);

        BlockDropItemEvent event = dropEvent(Material.DIAMOND_ORE, item(Material.DIAMOND, 1));
        listener.onBlockDropItems(event);
        listener.onBlockDropItemsFinished(event);

        assertEquals(1, event.getItems().getFirst().getItemStack().getAmount());
        assertNull(resolvedPosition);
        assertTrue(notifications.isEmpty());
        assertTrue(repository.findByPosition(POSITION).isPresent());
    }

    @Test
    void silkTouchBypassesMultiplierForBothOreKinds() {
        PaperOreBreakListener listener = listener(2.0);

        for (Material material : List.of(Material.IRON_ORE, Material.DIAMOND_ORE)) {
            Item silkDrop = item(material, 1);
            BlockDropItemEvent event = dropEvent(material, silkDrop);
            listener.onBlockDropItems(event);
            listener.onBlockDropItemsFinished(event);
            assertEquals(List.of(silkDrop), event.getItems());
        }

        assertNull(resolvedPosition);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void leavesAlreadyCancelledDropsUntouched() {
        PaperOreBreakListener listener = listener(2.0);
        Item oreDrop = item(Material.RAW_IRON, 1);
        ItemStack originalStack = oreDrop.getItemStack();
        BlockDropItemEvent event = dropEvent(Material.IRON_ORE, oreDrop);
        event.setCancelled(true);

        listener.onBlockDropItems(event);
        listener.onBlockDropItemsFinished(event);

        assertSame(originalStack, oreDrop.getItemStack());
        assertNull(resolvedPosition);
        assertTrue(notifications.isEmpty());
    }

    private PaperOreBreakListener listener(double multiplier) {
        return listener(this::resolveForest, fixedPolicy(multiplier));
    }

    private PaperOreBreakListener listener(BiomeResolver biomeResolver, OreDropPolicyProvider policyProvider) {
        OreOriginTrackingService originTracking = new OreOriginTrackingService(repository);
        OreDropService service = new OreDropService(
            originTracking,
            biomeResolver,
            policyProvider,
            () -> new SeasonId("minecraft:summer"),
            new OreDropMultiplierCalculator(() -> 0.5),
            new OreDropQuantityCalculator(() -> 0.5)
        );
        return new PaperOreBreakListener(
            service,
            new PaperVanillaDropScanner(),
            new PaperOreDropDeltaNotifier((player, message, color, tone) ->
                notifications.add(new Notification(message, color, tone)))
        );
    }

    private BiomeContext resolveForest(BlockPosition position) {
        resolvedPosition = position;
        return new BiomeContext(
            FOREST,
            new BiomeProfile(FOREST, new ClimateProfile(new Humidity(0.4), new Temperature(0.8)), new Fertility(0.7))
        );
    }

    private static OreDropPolicyProvider fixedPolicy(double multiplier) {
        OreDropOreRule rule = new OreDropOreRule(new OreDropMultiplierRange(multiplier, multiplier), Map.of());
        return biomeId -> new OreDropPolicy(biomeId, Map.of(IRON_ORE, rule, DIAMOND_ORE, rule));
    }

    private BlockDropItemEvent dropEvent(Material originalMaterial, Item... drops) {
        return new BlockDropItemEvent(
            block(),
            proxy(BlockState.class, Map.of("getType", originalMaterial)),
            player(),
            new ArrayList<>(List.of(drops))
        );
    }

    private Block block() {
        World world = (World) Proxy.newProxyInstance(
            World.class.getClassLoader(),
            new Class<?>[] { World.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getUID" -> WORLD_ID;
                case "createEntity" -> item(Material.STONE, 1);
                case "dropItemNaturally" -> throw new AssertionError("Drops must remain inside the event");
                default -> null;
            }
        );
        return (Block) Proxy.newProxyInstance(
            Block.class.getClassLoader(),
            new Class<?>[] { Block.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getType" -> Material.AIR;
                case "getWorld" -> world;
                case "getX" -> POSITION.x();
                case "getY" -> POSITION.y();
                case "getZ" -> POSITION.z();
                case "getDrops" -> throw new AssertionError("Ore loot was already computed");
                default -> null;
            }
        );
    }

    private static Player player() {
        return proxy(Player.class, Map.of());
    }

    private static Item item(Material material, int amount) {
        ItemStack[] stack = { new FakeItemStack(material, amount) };
        int[] pickupDelay = { 0 };
        Vector[] velocity = { new Vector() };
        return (Item) Proxy.newProxyInstance(
            Item.class.getClassLoader(),
            new Class<?>[] { Item.class },
            (proxy, method, arguments) -> switch (method.getName()) {
                case "getItemStack" -> stack[0];
                case "setItemStack" -> {
                    stack[0] = (ItemStack) arguments[0];
                    yield null;
                }
                case "getPickupDelay" -> pickupDelay[0];
                case "setPickupDelay" -> {
                    pickupDelay[0] = (int) arguments[0];
                    yield null;
                }
                case "getVelocity" -> velocity[0];
                case "setVelocity" -> {
                    velocity[0] = (Vector) arguments[0];
                    yield null;
                }
                case "equals" -> proxy == arguments[0];
                case "hashCode" -> System.identityHashCode(proxy);
                default -> null;
            }
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

    private static final class RecordingOriginRepository implements OreOriginRepository {
        private OreOrigin origin;

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
        }

        @Override
        public void applyChanges(
            Collection<BlockPosition> removedPositions,
            Collection<OreOrigin> savedOrigins
        ) {
            removedPositions.forEach(this::removeByPosition);
            savedOrigins.forEach(this::save);
        }

    }

    private record Notification(String message, TextColor color, OreDropDeltaTone tone) {}

    private static final class FakeItemStack extends ItemStack {
        private final Material type;
        private int amount;

        private FakeItemStack(Material type, int amount) {
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
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            return new FakeItemStack(type, amount);
        }
    }
}
