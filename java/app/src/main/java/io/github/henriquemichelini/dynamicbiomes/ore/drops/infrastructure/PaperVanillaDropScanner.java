package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

final class PaperVanillaDropScanner {

    PaperVanillaDropScan scan(List<Item> drops, Material blockMaterial) {
        Material oreProduct = productFor(blockMaterial);
        List<Item> oreItems = new ArrayList<>();
        int quantity = 0;

        for (Item drop : drops) {
            ItemStack stack = drop.getItemStack();
            if (stack.getType() == blockMaterial) {
                return new PaperVanillaDropScan(true, 0, List.of());
            }
            if (stack.getType() == oreProduct) {
                oreItems.add(drop);
                quantity = Math.addExact(quantity, stack.getAmount());
            }
        }

        return new PaperVanillaDropScan(false, quantity, oreItems);
    }

    private static Material productFor(Material blockMaterial) {
        return switch (blockMaterial) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> Material.COAL;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.RAW_COPPER;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.RAW_IRON;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.RAW_GOLD;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> Material.REDSTONE;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> Material.LAPIS_LAZULI;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> Material.DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> Material.EMERALD;
            default -> throw new IllegalArgumentException("Unsupported ore block: " + blockMaterial);
        };
    }
}

record PaperVanillaDropScan(boolean wouldDropOriginalBlock, int quantity, List<Item> oreItems) {

    void adjustQuantity(List<Item> drops, World world, int delta) {
        if (delta < 0) {
            removeQuantity(drops, -delta);
        } else {
            addQuantity(drops, world, delta);
        }
    }

    private void removeQuantity(List<Item> drops, int remaining) {
        for (Item item : oreItems.reversed()) {
            ItemStack stack = item.getItemStack();
            if (remaining < stack.getAmount()) {
                setAmount(item, stack, stack.getAmount() - remaining);
                return;
            }
            drops.remove(item);
            remaining -= stack.getAmount();
            if (remaining == 0) return;
        }
    }

    private void addQuantity(List<Item> drops, World world, int remaining) {
        for (Item item : oreItems) {
            ItemStack stack = item.getItemStack();
            int added = Math.min(remaining, stack.getMaxStackSize() - stack.getAmount());
            if (added <= 0) continue;

            setAmount(item, stack, stack.getAmount() + added);
            remaining -= added;
            if (remaining == 0) return;
        }

        Item representative = oreItems.getFirst();
        ItemStack template = representative.getItemStack();
        while (remaining > 0) {
            int amount = Math.min(remaining, template.getMaxStackSize());
            Item extra = world.createEntity(representative.getLocation(), Item.class);
            setAmount(extra, template, amount);
            extra.setPickupDelay(representative.getPickupDelay());
            extra.setVelocity(representative.getVelocity());
            drops.add(extra);
            remaining -= amount;
        }
    }

    private static void setAmount(Item item, ItemStack original, int amount) {
        ItemStack adjusted = original.clone();
        adjusted.setAmount(amount);
        item.setItemStack(adjusted);
    }
}
