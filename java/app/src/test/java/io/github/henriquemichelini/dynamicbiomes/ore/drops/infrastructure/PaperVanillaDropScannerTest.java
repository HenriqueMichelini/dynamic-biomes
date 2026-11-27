package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PaperVanillaDropScannerTest {

    private final PaperVanillaDropScanner scanner = new PaperVanillaDropScanner();

    @Test
    void detectsOriginalOreBlockEvenWhenOtherItemsWouldDrop() {
        PaperVanillaDropScan scan = scanner.scan(
            List.of(drop(Material.RAW_IRON, 2), drop(Material.IRON_ORE, 1)),
            Material.IRON_ORE
        );

        assertTrue(scan.wouldDropOriginalBlock());
    }

    @Test
    void sumsOnlyExpectedOreProductAndKeepsItsEntities() {
        Item firstOreDrop = drop(Material.RAW_IRON, 2);
        Item secondOreDrop = drop(Material.RAW_IRON, 3);

        PaperVanillaDropScan scan = scanner.scan(
            List.of(firstOreDrop, drop(Material.TORCH, 4), secondOreDrop),
            Material.DEEPSLATE_IRON_ORE
        );

        assertFalse(scan.wouldDropOriginalBlock());
        assertEquals(5, scan.quantity());
        assertEquals(List.of(firstOreDrop, secondOreDrop), scan.oreItems());
    }

    @Test
    void emptyCapturedDropsHaveZeroQuantity() {
        PaperVanillaDropScan scan = scanner.scan(List.of(), Material.IRON_ORE);

        assertFalse(scan.wouldDropOriginalBlock());
        assertEquals(0, scan.quantity());
        assertTrue(scan.oreItems().isEmpty());
    }

    private static Item drop(Material material, int amount) {
        ItemStack stack = new ItemStack() {
            @Override
            public Material getType() {
                return material;
            }

            @Override
            public int getAmount() {
                return amount;
            }
        };
        return (Item) Proxy.newProxyInstance(
            Item.class.getClassLoader(),
            new Class<?>[] { Item.class },
            (proxy, method, arguments) -> {
                if (method.getName().equals("getItemStack")) return stack;
                if (method.getName().equals("equals")) return proxy == arguments[0];
                return null;
            }
        );
    }
}
