package io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.application.TreeGrowthService;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthDecision;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

public final class PaperTreeGrowthListener implements Listener {
    private final TreeGrowthService treeGrowthService;

    public PaperTreeGrowthListener(@NonNull TreeGrowthService treeGrowthService) {
        this.treeGrowthService = treeGrowthService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        if (
            event.isCancelled() || event.isFromBonemeal() || event.getPlayer() != null ||
            !isSupportedTree(event.getSpecies())
        ) {
            return;
        }

        Location location = event.getLocation();
        BlockPosition position = new BlockPosition(
            new WorldReference(event.getWorld().getUID()),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        if (treeGrowthService.decideNaturalGrowth(position) == TreeGrowthDecision.CANCEL_GROWTH) {
            event.setCancelled(true);
        }
    }

    private static boolean isSupportedTree(TreeType type) {
        return switch (type) {
            case TREE, BIG_TREE, REDWOOD, TALL_REDWOOD, BIRCH, JUNGLE, SMALL_JUNGLE,
                 COCOA_TREE, SWAMP, ACACIA, DARK_OAK, MEGA_REDWOOD, MEGA_PINE,
                 TALL_BIRCH, MANGROVE, TALL_MANGROVE, CHERRY, PALE_OAK, PALE_OAK_CREAKING -> true;
            default -> false;
        };
    }
}
