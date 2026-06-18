package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Collection;
import java.util.Optional;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class PaperOreBreakListener implements Listener {

    private final OreDropService dropService;
    private final OreOriginTrackingService originTracking;
    private final PaperVanillaDropScanner vanillaDropScanner;
    private final OreDropDeltaNotifier deltaNotifier;

    public PaperOreBreakListener(
        OreDropService dropService,
        OreOriginTrackingService originTracking
    ) {
        this(
            dropService,
            originTracking,
            new PaperVanillaDropScanner(),
            new PaperOreDropDeltaNotifier()
        );
    }

    PaperOreBreakListener(
        @NonNull OreDropService dropService,
        @NonNull OreOriginTrackingService originTracking,
        @NonNull PaperVanillaDropScanner vanillaDropScanner,
        @NonNull OreDropDeltaNotifier deltaNotifier
    ) {
        this.dropService = dropService;
        this.originTracking = originTracking;
        this.vanillaDropScanner = vanillaDropScanner;
        this.deltaNotifier = deltaNotifier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        Optional<OreKind> oreKindResult = PaperOreMaterialMapper.oreKindFor(
            material
        );

        if (oreKindResult.isEmpty()) return;

        OreKind oreKind = oreKindResult.orElseThrow();

        BlockPosition position = new BlockPosition(
            new WorldReference(block.getWorld().getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );

        try {
            Collection<ItemStack> vanillaDrops = block.getDrops(
                player.getInventory().getItemInMainHand(),
                player
            );

            PaperVanillaDropScan vanillaDropScan = vanillaDropScanner.scan(
                vanillaDrops,
                material
            );

            if (vanillaDropScan.empty()) return;

            if (vanillaDropScan.wouldDropOriginalBlock()) return;

            int vanillaFortuneQuantity = vanillaDropScan.quantity();

            int quantity = dropService.calculateDrops(
                position,
                oreKind,
                vanillaFortuneQuantity
            );

            if (quantity == vanillaFortuneQuantity) return;

            int delta = quantity - vanillaFortuneQuantity;

            deltaNotifier.notifyDelta(player, material, delta);

            event.setDropItems(false);

            if (quantity <= 0) return;

            ItemStack boostedDrop = vanillaDropScan
                .representativeDrop()
                .orElseThrow();

            boostedDrop.setAmount(quantity);

            block
                .getWorld()
                .dropItemNaturally(block.getLocation(), boostedDrop);
        } finally {
            originTracking.clearTrackedOrigin(position);
        }
    }
}
