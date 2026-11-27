package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.ExplosionResult;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class PaperOreRemovalListener implements Listener {
    private final OreOriginTrackingService originTracking;
    private final Consumer<Runnable> scheduleNextTick;

    public PaperOreRemovalListener(OreOriginTrackingService originTracking, Consumer<Runnable> scheduleNextTick) {
        this.originTracking = originTracking;
        this.scheduleNextTick = scheduleNextTick;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!PaperOreMaterialMapper.isSupportedOre(block.getType())) return;

        BlockPosition position = positionOf(block);
        if (originTracking.originAt(position).isEmpty()) return;

        // The captured drop event still needs the original ore origin for this break.
        scheduleNextTick.accept(() -> {
            if (!event.isCancelled() && !PaperOreMaterialMapper.isSupportedOre(block.getType())) {
                originTracking.clearTrackedOrigin(position);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        clearExplodedOres(event.getExplosionResult(), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        clearExplodedOres(event.getExplosionResult(), event.blockList());
    }

    private void clearExplodedOres(ExplosionResult result, List<Block> blocks) {
        if (result != ExplosionResult.DESTROY && result != ExplosionResult.DESTROY_WITH_DECAY) return;

        List<BlockPosition> positions = blocks.stream()
            .filter(block -> PaperOreMaterialMapper.isSupportedOre(block.getType()))
            .map(PaperOreRemovalListener::positionOf)
            .toList();
        if (!positions.isEmpty()) originTracking.clearTrackedOrigins(positions);
    }

    private static BlockPosition positionOf(Block block) {
        return new BlockPosition(
            new WorldReference(block.getWorld().getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
    }
}
