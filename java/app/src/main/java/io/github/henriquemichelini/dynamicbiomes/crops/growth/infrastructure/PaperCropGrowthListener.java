package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import lombok.NonNull;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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

        BlockState proposedState = event.getNewState();
        // Paper reports both random ticks and bone meal via BlockGrowEvent.
        // Natural crop ticks propose exactly one stage on the same material.
        if (!(block.getBlockData() instanceof Ageable current &&
            proposedState.getBlockData() instanceof Ageable proposed &&
            current.getMaterial() == proposed.getMaterial() &&
            proposed.getAge() - current.getAge() == 1)) {
            return;
        }

        BlockPosition position = new BlockPosition(
            new WorldReference(block.getWorld().getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
        int currentAge = current.getAge();
        int stages = cropGrowthService.naturalGrowthStages(
            position,
            cropKind,
            proposed.getMaximumAge() - currentAge
        );
        if (stages == 0) {
            event.setCancelled(true);
        } else if (stages > 1) {
            proposed.setAge(currentAge + stages);
            proposedState.setBlockData(proposed);
        }
    }
}
