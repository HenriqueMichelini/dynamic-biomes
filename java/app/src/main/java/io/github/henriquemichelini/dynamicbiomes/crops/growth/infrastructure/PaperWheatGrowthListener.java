package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public final class PaperWheatGrowthListener implements Listener {
    private final CropGrowthService cropGrowthService;

    public PaperWheatGrowthListener(CropGrowthService cropGrowthService) {
        this.cropGrowthService = cropGrowthService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.WHEAT) {
            return;
        }

        CropGrowthDecision decision = cropGrowthService.decideNaturalWheatGrowth(
            new BlockPosition(
                new WorldReference(block.getWorld().getUID()),
                block.getX(),
                block.getY(),
                block.getZ()
            )
        );
        if (decision == CropGrowthDecision.CANCEL_GROWTH) {
            event.setCancelled(true);
        }
    }
}
