package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class PaperOreBreakListener implements Listener {
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");

    private final OreDropService dropService;
    private final OreOriginTrackingService originTracking;

    public PaperOreBreakListener(
        OreDropService dropService,
        OreOriginTrackingService originTracking
    ) {
        this.dropService = dropService;
        this.originTracking = originTracking;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.IRON_ORE) {
            return;
        }

        BlockPosition position = new BlockPosition(
            new WorldReference(block.getWorld().getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
        
        Collection<ItemStack> vanillaDrops = block.getDrops(
            event.getPlayer().getInventory().getItemInMainHand(),
            event.getPlayer()
        );
        
        int vanillaFortuneQuantity = vanillaDrops.stream()
            .mapToInt(ItemStack::getAmount)
            .sum();

        try {
            int quantity = dropService.calculateDrops(
                position,
                IRON_ORE,
                vanillaFortuneQuantity
            );
            if (quantity != vanillaFortuneQuantity && !vanillaDrops.isEmpty()) {
                event.setDropItems(false);
                if (quantity > 0) {
                    ItemStack boostedDrop = vanillaDrops.iterator().next().clone();
                    boostedDrop.setAmount(quantity);
                    block.getWorld().dropItemNaturally(block.getLocation(), boostedDrop);
                }
            }
        } finally {
            originTracking.clearTrackedOrigin(position);
        }
    }
}
