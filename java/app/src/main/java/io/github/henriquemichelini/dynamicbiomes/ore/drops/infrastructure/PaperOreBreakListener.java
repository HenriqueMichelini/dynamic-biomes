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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class PaperOreBreakListener implements Listener {

    private static final TextColor POSITIVE_DELTA_COLOR = TextColor.color(
        0x90EE90
    );
    private static final TextColor NEGATIVE_DELTA_COLOR = NamedTextColor.RED;

    private final OreDropService dropService;
    private final OreOriginTrackingService originTracking;
    private final OreDropActionBarNotifier actionBarNotifier;

    public PaperOreBreakListener(
        OreDropService dropService,
        OreOriginTrackingService originTracking
    ) {
        this(dropService, originTracking, (player, message, color, tone) -> {
            player.sendActionBar(Component.text(message, color));
            player.playSound(player.getLocation(), soundFor(tone), 1.0f, 1.0f);
        });
    }

    PaperOreBreakListener(
        @NonNull OreDropService dropService,
        @NonNull OreOriginTrackingService originTracking,
        @NonNull OreDropActionBarNotifier actionBarNotifier
    ) {
        this.dropService = dropService;
        this.originTracking = originTracking;
        this.actionBarNotifier = actionBarNotifier;
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

            if (vanillaDrops.isEmpty()) return;

            VanillaDropScan vanillaDropScan = scanVanillaDrops(
                vanillaDrops,
                material
            );

            if (vanillaDropScan.wouldDropOreBlock()) return;

            int vanillaFortuneQuantity = vanillaDropScan.quantity();

            int quantity = dropService.calculateDrops(
                position,
                oreKind,
                vanillaFortuneQuantity
            );

            if (quantity == vanillaFortuneQuantity) return;

            int delta = quantity - vanillaFortuneQuantity;

            actionBarNotifier.send(
                player,
                formatDeltaMessage(delta, material),
                deltaColor(delta),
                deltaTone(delta)
            );

            event.setDropItems(false);

            if (quantity <= 0) return;

            ItemStack boostedDrop = vanillaDrops.iterator().next().clone();

            boostedDrop.setAmount(quantity);

            block
                .getWorld()
                .dropItemNaturally(block.getLocation(), boostedDrop);
        } finally {
            originTracking.clearTrackedOrigin(position);
        }
    }

    private static String formatDeltaMessage(int delta, Material material) {
        String oreName = material.name().toLowerCase().replace('_', ' ');
        if (delta > 0) {
            return "+" + delta + " " + oreName + " extra!";
        }
        return delta + " " + oreName;
    }

    private static TextColor deltaColor(int delta) {
        return delta > 0 ? POSITIVE_DELTA_COLOR : NEGATIVE_DELTA_COLOR;
    }

    private static OreDropDeltaTone deltaTone(int delta) {
        return delta > 0
            ? OreDropDeltaTone.POSITIVE
            : OreDropDeltaTone.NEGATIVE;
    }

    private static Sound soundFor(OreDropDeltaTone tone) {
        return switch (tone) {
            case POSITIVE -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case NEGATIVE -> Sound.ITEM_WOLF_ARMOR_REPAIR;
        };
    }

    private record VanillaDropScan(boolean wouldDropOreBlock, int quantity) {}

    private static VanillaDropScan scanVanillaDrops(
        Collection<ItemStack> drops,
        Material blockMaterial
    ) {
        boolean wouldDropOreBlock = false;
        int quantity = 0;

        for (ItemStack drop : drops) {
            if (drop.getType() == blockMaterial) {
                wouldDropOreBlock = true;
            }

            quantity += drop.getAmount();
        }

        return new VanillaDropScan(wouldDropOreBlock, quantity);
    }
}

@FunctionalInterface
interface OreDropActionBarNotifier {
    void send(
        Player player,
        String message,
        TextColor color,
        OreDropDeltaTone tone
    );
}

enum OreDropDeltaTone {
    POSITIVE,
    NEGATIVE,
}
