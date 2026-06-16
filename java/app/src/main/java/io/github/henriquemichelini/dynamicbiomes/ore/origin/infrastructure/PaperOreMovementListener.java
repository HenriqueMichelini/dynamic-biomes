package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public final class PaperOreMovementListener implements Listener {
    private final OreOriginTrackingService originTracking;

    public PaperOreMovementListener(OreOriginTrackingService originTracking) {
        this.originTracking = originTracking;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace direction = event.getDirection();
        for (Block block : event.getBlocks()) {
            if (!PaperOreMaterialMapper.isSupportedOre(block.getType())) {
                continue;
            }
            BlockPosition source = blockPosition(block);
            BlockPosition destination = new BlockPosition(
                source.world(),
                source.x() + direction.getModX(),
                source.y() + direction.getModY(),
                source.z() + direction.getModZ()
            );
            originTracking.moveTrackedOrigin(source, destination);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace opposite = event.getDirection().getOppositeFace();
        for (Block block : event.getBlocks()) {
            if (!PaperOreMaterialMapper.isSupportedOre(block.getType())) {
                continue;
            }
            BlockPosition source = blockPosition(block);
            BlockPosition destination = new BlockPosition(
                source.world(),
                source.x() + opposite.getModX(),
                source.y() + opposite.getModY(),
                source.z() + opposite.getModZ()
            );
            originTracking.moveTrackedOrigin(source, destination);
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
