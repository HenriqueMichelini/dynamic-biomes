package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public final class PaperOreMovementListener implements Listener {
    private final OreOriginTrackingService originTracking;

    public PaperOreMovementListener(OreOriginTrackingService originTracking) {
        this.originTracking = originTracking;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        trackMovement(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        trackMovement(event, event.getBlocks());
    }

    private void trackMovement(BlockPistonEvent event, List<Block> movedBlocks) {
        // For a nonempty movement Paper supplies the direction the blocks travel.
        BlockFace direction = event.getDirection();
        Map<BlockPosition, BlockPosition> movements = new LinkedHashMap<>();
        for (Block block : movedBlocks) {
            if (!PaperOreMaterialMapper.isSupportedOre(block.getType())) continue;

            BlockPosition source = blockPosition(block);
            movements.put(source, new BlockPosition(
                source.world(),
                source.x() + direction.getModX(),
                source.y() + direction.getModY(),
                source.z() + direction.getModZ()
            ));
        }

        try {
            originTracking.moveTrackedOrigins(movements);
        } catch (RuntimeException failure) {
            // An ore movement must not proceed without its committed origin update.
            event.setCancelled(true);
            throw failure;
        }
    }

    private static BlockPosition blockPosition(Block block) {
        return new BlockPosition(
            new WorldReference(block.getWorld().getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
    }
}
