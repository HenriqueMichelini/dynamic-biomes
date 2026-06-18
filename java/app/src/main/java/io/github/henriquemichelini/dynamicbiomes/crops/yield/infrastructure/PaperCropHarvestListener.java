package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.application.CropYieldService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
    private final CropYieldDropper dropper;
    private final CropYieldActionBarNotifier actionBarNotifier;

    public PaperCropHarvestListener(@NonNull CropYieldService cropYieldService) {
        this(cropYieldService, World::dropItemNaturally, (player, message, color, tone) -> {
            player.sendActionBar(Component.text(message, color));
            player.playSound(player.getLocation(), soundFor(tone), 1.0f, 1.0f);
        });
    }

    PaperCropHarvestListener(
        @NonNull CropYieldService cropYieldService,
        @NonNull CropYieldDropper dropper,
        @NonNull CropYieldActionBarNotifier actionBarNotifier
    ) {
        this.cropYieldService = cropYieldService;
        this.dropper = dropper;
        this.actionBarNotifier = actionBarNotifier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        CropKind cropKind = PaperCropMaterialMapper
            .cropKindFor(block.getType())
            .orElse(null);
        if (cropKind == null || !isMature(block)) {
            return;
        }

        Collection<ItemStack> vanillaDrops = block.getDrops(
            event.getPlayer().getInventory().getItemInMainHand(),
            event.getPlayer()
        );
        Material produceMaterial = PRODUCE_MATERIALS.get(cropKind);
        int vanillaProduceQuantity = vanillaDrops
            .stream()
            .filter(drop -> drop.getType() == produceMaterial)
            .mapToInt(ItemStack::getAmount)
            .sum();
        if (vanillaProduceQuantity == 0) {
            return;
        }

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
        if (adjustedProduceQuantity == vanillaProduceQuantity) {
            return;
        }

        int delta = adjustedProduceQuantity - vanillaProduceQuantity;
        actionBarNotifier.send(
            event.getPlayer(),
            formatDeltaMessage(delta, produceMaterial),
            deltaColor(delta),
            deltaTone(delta)
        );
        event.setDropItems(false);
        ItemStack produceTemplate = null;
        for (ItemStack vanillaDrop : vanillaDrops) {
            if (vanillaDrop.getType() == produceMaterial) {
                if (produceTemplate == null) {
                    produceTemplate = vanillaDrop.clone();
                }
                continue;
            }
            dropper.drop(block.getWorld(), block.getLocation(), vanillaDrop.clone());
        }
        if (adjustedProduceQuantity > 0 && produceTemplate != null) {
            dropInBatches(
                block.getWorld(),
                block.getLocation(),
                produceTemplate,
                adjustedProduceQuantity
            );
        }
    }

    private static boolean isMature(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() == ageable.getMaximumAge();
    }

    private void dropInBatches(
        World world,
        Location location,
        ItemStack template,
        int quantity
    ) {
        int remaining = quantity;
        int maxStackSize = template.getMaxStackSize();
        while (remaining > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(remaining, maxStackSize));
            dropper.drop(world, location, stack);
            remaining -= stack.getAmount();
        }
    }

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
interface CropYieldDropper {
    void drop(World world, Location location, ItemStack itemStack);
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
