package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PaperVanillaDropScannerTest {

    private final PaperVanillaDropScanner scanner = new PaperVanillaDropScanner();

    @Test
    void detectsWhenVanillaDropsContainOriginalOreBlock() {
        PaperVanillaDropScan scan = scanner.scan(
            List.of(new FakeItemStack(Material.IRON_ORE)),
            Material.IRON_ORE
        );

        assertFalse(scan.empty());
        assertTrue(scan.wouldDropOriginalBlock());
        assertEquals(1, scan.quantity());
    }

    @Test
    void sumsVanillaDropQuantityAndKeepsRepresentativeDropCopy() {
        FakeItemStack firstDrop = new FakeItemStack(Material.RAW_IRON, 2);

        PaperVanillaDropScan scan = scanner.scan(
            List.of(firstDrop, new FakeItemStack(Material.RAW_IRON, 3)),
            Material.IRON_ORE
        );

        ItemStack representativeDrop = scan.representativeDrop().orElseThrow();

        assertFalse(scan.empty());
        assertFalse(scan.wouldDropOriginalBlock());
        assertEquals(5, scan.quantity());
        assertEquals(Material.RAW_IRON, representativeDrop.getType());
        assertEquals(2, representativeDrop.getAmount());
        assertNotSame(firstDrop, representativeDrop);
    }

    @Test
    void reportsNoRepresentativeDropForEmptyVanillaDrops() {
        PaperVanillaDropScan scan = scanner.scan(List.of(), Material.IRON_ORE);

        assertTrue(scan.empty());
        assertFalse(scan.wouldDropOriginalBlock());
        assertEquals(0, scan.quantity());
        assertTrue(scan.representativeDrop().isEmpty());
    }

    private static final class FakeItemStack extends ItemStack {
        private final Material type;
        private int amount;

        FakeItemStack(Material type) {
            this(type, 1);
        }

        FakeItemStack(Material type, int amount) {
            super();
            this.type = type;
            this.amount = amount;
        }

        @Override
        public Material getType() {
            return type;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            return new FakeItemStack(type, amount);
        }
    }
}
