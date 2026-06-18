package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import java.util.Collection;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class PaperVanillaDropScanner {

    PaperVanillaDropScan scan(
        Collection<ItemStack> drops,
        Material blockMaterial
    ) {
        boolean wouldDropOriginalBlock = false;
        int quantity = 0;
        ItemStack representativeDrop = null;

        for (ItemStack drop : drops) {
            if (representativeDrop == null) {
                representativeDrop = drop.clone();
            }

            if (drop.getType() == blockMaterial) {
                wouldDropOriginalBlock = true;
            }

            quantity += drop.getAmount();
        }

        return new PaperVanillaDropScan(
            drops.isEmpty(),
            wouldDropOriginalBlock,
            quantity,
            Optional.ofNullable(representativeDrop)
        );
    }
}

record PaperVanillaDropScan(
    boolean empty,
    boolean wouldDropOriginalBlock,
    int quantity,
    Optional<ItemStack> representativeDrop
) {}
