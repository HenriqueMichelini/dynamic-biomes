package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;

public final class PaperOreBreakListener implements Listener {

    private final OreDropService dropService;
    private final PaperVanillaDropScanner vanillaDropScanner;
    private final OreDropDeltaNotifier deltaNotifier;
    private final Map<BlockDropItemEvent, Integer> pendingDeltas = new IdentityHashMap<>();

    public PaperOreBreakListener(OreDropService dropService) {
        this(
            dropService,
            new PaperVanillaDropScanner(),
            new PaperOreDropDeltaNotifier()
        );
    }

    PaperOreBreakListener(
        @NonNull OreDropService dropService,
        @NonNull PaperVanillaDropScanner vanillaDropScanner,
        @NonNull OreDropDeltaNotifier deltaNotifier
    ) {
        this.dropService = dropService;
        this.vanillaDropScanner = vanillaDropScanner;
        this.deltaNotifier = deltaNotifier;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItems(BlockDropItemEvent event) {
        if (event.isCancelled()) return;

        Material material = event.getBlockState().getType();
        Optional<OreKind> oreKind = PaperOreMaterialMapper.oreKindFor(material);
        if (oreKind.isEmpty()) return;

        List<Item> drops = event.getItems();
        PaperVanillaDropScan scan = vanillaDropScanner.scan(drops, material);
        if (scan.wouldDropOriginalBlock() || scan.quantity() == 0) return;

        Block block = event.getBlock();
        int adjustedQuantity = dropService.calculateDrops(
            positionOf(block),
            oreKind.orElseThrow(),
            scan.quantity()
        );
        int delta = adjustedQuantity - scan.quantity();
        if (delta == 0) return;

        scan.adjustQuantity(drops, block.getWorld(), delta);
        pendingDeltas.put(event, delta);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDropItemsFinished(BlockDropItemEvent event) {
        Integer delta = pendingDeltas.remove(event);
        if (delta != null && !event.isCancelled()) {
            deltaNotifier.notifyDelta(event.getPlayer(), event.getBlockState().getType(), delta);
        }
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
