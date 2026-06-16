package io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class PaperOreMaterialMapperTest {

    @Test
    void mapsConfiguredOverworldOreMaterialsToOreKinds() {
        Map<Material, OreKind> expected = Map.ofEntries(
            Map.entry(Material.COAL_ORE, new OreKind("minecraft:coal_ore")),
            Map.entry(
                Material.DEEPSLATE_COAL_ORE,
                new OreKind("minecraft:deepslate_coal_ore")
            ),
            Map.entry(Material.COPPER_ORE, new OreKind("minecraft:copper_ore")),
            Map.entry(
                Material.DEEPSLATE_COPPER_ORE,
                new OreKind("minecraft:deepslate_copper_ore")
            ),
            Map.entry(Material.IRON_ORE, new OreKind("minecraft:iron_ore")),
            Map.entry(
                Material.DEEPSLATE_IRON_ORE,
                new OreKind("minecraft:deepslate_iron_ore")
            ),
            Map.entry(Material.GOLD_ORE, new OreKind("minecraft:gold_ore")),
            Map.entry(
                Material.DEEPSLATE_GOLD_ORE,
                new OreKind("minecraft:deepslate_gold_ore")
            ),
            Map.entry(Material.REDSTONE_ORE, new OreKind("minecraft:redstone_ore")),
            Map.entry(
                Material.DEEPSLATE_REDSTONE_ORE,
                new OreKind("minecraft:deepslate_redstone_ore")
            ),
            Map.entry(Material.LAPIS_ORE, new OreKind("minecraft:lapis_ore")),
            Map.entry(
                Material.DEEPSLATE_LAPIS_ORE,
                new OreKind("minecraft:deepslate_lapis_ore")
            ),
            Map.entry(Material.DIAMOND_ORE, new OreKind("minecraft:diamond_ore")),
            Map.entry(
                Material.DEEPSLATE_DIAMOND_ORE,
                new OreKind("minecraft:deepslate_diamond_ore")
            ),
            Map.entry(Material.EMERALD_ORE, new OreKind("minecraft:emerald_ore")),
            Map.entry(
                Material.DEEPSLATE_EMERALD_ORE,
                new OreKind("minecraft:deepslate_emerald_ore")
            )
        );

        expected.forEach((material, oreKind) ->
            assertEquals(
                oreKind,
                PaperOreMaterialMapper.oreKindFor(material).orElseThrow()
            )
        );
    }

    @Test
    void rejectsNonOreMaterials() {
        assertTrue(PaperOreMaterialMapper.oreKindFor(Material.STONE).isEmpty());
        assertFalse(PaperOreMaterialMapper.isSupportedOre(Material.STONE));
    }
}
