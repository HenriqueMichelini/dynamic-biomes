package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public final class PaperCropGrowthListener implements Listener {

    private final CropGrowthService cropGrowthService;

    public PaperCropGrowthListener(@NonNull CropGrowthService cropGrowthService) {
        this.cropGrowthService = cropGrowthService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        CropKind cropKind = PaperCropMaterialMapper
            .cropKindFor(block.getType())
            .orElse(null);
        if (cropKind == null) {
            return;
        }

        CropGrowthDecision decision = cropGrowthService.decideNaturalGrowth(
            new BlockPosition(
                new WorldReference(block.getWorld().getUID()),
                block.getX(),
                block.getY(),
                block.getZ()
            ),
            cropKind
        );
        if (decision == CropGrowthDecision.CANCEL_GROWTH) {
            event.setCancelled(true);
        }
    }
}
