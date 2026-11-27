package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.application.CropYieldService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public final class PaperCropHarvestListener implements Listener {
    private static final TextColor POSITIVE_DELTA_COLOR = TextColor.color(
        0x90EE90
    );
    private static final TextColor NEGATIVE_DELTA_COLOR = NamedTextColor.RED;
    private static final Map<CropKind, Material> PRODUCE_MATERIALS = new EnumMap<>(
        Map.of(
            CropKind.WHEAT,
            Material.WHEAT,
            CropKind.CARROTS,
            Material.CARROT,
            CropKind.POTATOES,
            Material.POTATO,
            CropKind.BEETROOT,
            Material.BEETROOT
        )
    );

    private final CropYieldService cropYieldService;
    private final CropYieldActionBarNotifier actionBarNotifier;
    private final Map<BlockDropItemEvent, YieldDelta> pendingFeedback = new WeakHashMap<>();

    public PaperCropHarvestListener(@NonNull CropYieldService cropYieldService) {
        this(cropYieldService, (player, message, color, tone) -> {
            player.sendActionBar(Component.text(message, color));
            player.playSound(player.getLocation(), soundFor(tone), 1.0f, 1.0f);
        });
    }

    PaperCropHarvestListener(
        @NonNull CropYieldService cropYieldService,
        @NonNull CropYieldActionBarNotifier actionBarNotifier
    ) {
        this.cropYieldService = cropYieldService;
        this.actionBarNotifier = actionBarNotifier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        BlockState harvestedState = event.getBlockState();
        CropKind cropKind = PaperCropMaterialMapper
            .cropKindFor(harvestedState.getType())
            .orElse(null);
        if (cropKind == null || !isMature(harvestedState)) {
            return;
        }

        Material produceMaterial = PRODUCE_MATERIALS.get(cropKind);
        List<Item> capturedDrops = event.getItems();
        List<Item> produceDrops = capturedDrops.stream()
            .filter(drop -> drop.getItemStack().getType() == produceMaterial)
            .toList();
        int vanillaProduceQuantity = produceDrops.stream()
            .mapToInt(drop -> drop.getItemStack().getAmount())
            .sum();
        if (vanillaProduceQuantity == 0) {
            return;
        }

        Block block = event.getBlock();
        int adjustedProduceQuantity = cropYieldService.calculateProduceQuantity(
            new BlockPosition(
                new WorldReference(block.getWorld().getUID()),
                block.getX(),
                block.getY(),
                block.getZ()
            ),
            cropKind,
            vanillaProduceQuantity
        );
        int delta = adjustedProduceQuantity - vanillaProduceQuantity;
        if (delta == 0) {
            return;
        }

        if (delta > 0) {
            addProduce(capturedDrops, produceDrops, delta);
        } else {
            removeProduce(capturedDrops, produceMaterial, -delta);
        }
        pendingFeedback.put(event, new YieldDelta(delta, produceMaterial));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void reportYieldDelta(BlockDropItemEvent event) {
        YieldDelta change = pendingFeedback.remove(event);
        if (change == null || event.isCancelled()) {
            return;
        }
        int delta = change.amount();
        actionBarNotifier.send(
            event.getPlayer(),
            formatDeltaMessage(delta, change.material()),
            deltaColor(delta),
            deltaTone(delta)
        );
    }

    private static boolean isMature(BlockState state) {
        return state.getBlockData() instanceof Ageable ageable &&
            ageable.getAge() == ageable.getMaximumAge();
    }

    private static void addProduce(List<Item> drops, List<Item> produceDrops, int remaining) {
        for (Item item : produceDrops) {
            ItemStack stack = item.getItemStack();
            int added = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
            if (added <= 0) {
                continue;
            }
            stack.setAmount(stack.getAmount() + added);
            item.setItemStack(stack);
            remaining -= added;
            if (remaining == 0) {
                return;
            }
        }

        Item source = produceDrops.getFirst();
        ItemStack template = source.getItemStack();
        int maximumStackSize = template.getMaxStackSize();
        while (remaining > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(remaining, maximumStackSize));
            Item extraDrop = source.getWorld().createEntity(source.getLocation(), Item.class);
            extraDrop.setItemStack(stack);
            extraDrop.setPickupDelay(source.getPickupDelay());
            extraDrop.setVelocity(source.getVelocity());
            drops.add(extraDrop);
            remaining -= stack.getAmount();
        }
    }

    private static void removeProduce(List<Item> drops, Material material, int remaining) {
        ListIterator<Item> iterator = drops.listIterator(drops.size());
        while (remaining > 0 && iterator.hasPrevious()) {
            Item item = iterator.previous();
            ItemStack stack = item.getItemStack();
            if (stack.getType() != material) {
                continue;
            }
            int removed = Math.min(remaining, stack.getAmount());
            if (removed == stack.getAmount()) {
                iterator.remove();
            } else {
                stack.setAmount(stack.getAmount() - removed);
                item.setItemStack(stack);
            }
            remaining -= removed;
        }
    }

    private record YieldDelta(int amount, Material material) {}

    private static String formatDeltaMessage(int delta, Material material) {
        String produceName = material.name().toLowerCase().replace('_', ' ');
        if (delta > 0) {
            return "+" + delta + " " + produceName + " extra!";
        }
        return delta + " " + produceName;
    }

    private static TextColor deltaColor(int delta) {
        return delta > 0 ? POSITIVE_DELTA_COLOR : NEGATIVE_DELTA_COLOR;
    }

    private static CropYieldDeltaTone deltaTone(int delta) {
        return delta > 0
            ? CropYieldDeltaTone.POSITIVE
            : CropYieldDeltaTone.NEGATIVE;
    }

    private static Sound soundFor(CropYieldDeltaTone tone) {
        return switch (tone) {
            case POSITIVE -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case NEGATIVE -> Sound.ITEM_WOLF_ARMOR_REPAIR;
        };
    }
}

@FunctionalInterface
interface CropYieldActionBarNotifier {
    void send(
        Player player,
        String message,
        TextColor color,
        CropYieldDeltaTone tone
    );
}

enum CropYieldDeltaTone {
    POSITIVE,
    NEGATIVE,
}
