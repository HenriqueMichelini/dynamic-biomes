package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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

        // Paper uses the same event for natural ticks and bone meal.
        // Natural growth proposes one stage on the same crop.
        if (!(block.getBlockData() instanceof Ageable current &&
            event.getNewState().getBlockData() instanceof Ageable proposed &&
            current.getMaterial() == proposed.getMaterial() &&
            proposed.getAge() - current.getAge() == 1)) {
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
