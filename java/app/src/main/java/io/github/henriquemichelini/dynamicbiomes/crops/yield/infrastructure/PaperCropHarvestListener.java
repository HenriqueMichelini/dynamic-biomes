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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class PaperCropHarvestListener implements Listener {
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

    public PaperCropHarvestListener(@NonNull CropYieldService cropYieldService) {
        this(cropYieldService, World::dropItemNaturally);
    }

    PaperCropHarvestListener(
        @NonNull CropYieldService cropYieldService,
        @NonNull CropYieldDropper dropper
    ) {
        this.cropYieldService = cropYieldService;
        this.dropper = dropper;
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
}

@FunctionalInterface
interface CropYieldDropper {
    void drop(World world, Location location, ItemStack itemStack);
}
