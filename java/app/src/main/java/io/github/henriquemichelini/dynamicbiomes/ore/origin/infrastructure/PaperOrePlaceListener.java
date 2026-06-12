package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class PaperOrePlaceListener implements Listener {
    private final OreOriginTrackingService originTracking;

    public PaperOrePlaceListener(OreOriginTrackingService originTracking) {
        this.originTracking = originTracking;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.IRON_ORE) {
            return;
        }

        originTracking.recordPlayerPlacedOre(
            new BlockPosition(
                new WorldReference(block.getWorld().getUID()),
                block.getX(),
                block.getY(),
                block.getZ()
            )
        );
    }
}
