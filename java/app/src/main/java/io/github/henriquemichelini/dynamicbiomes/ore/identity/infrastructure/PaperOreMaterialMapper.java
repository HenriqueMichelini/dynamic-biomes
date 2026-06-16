package io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;

public final class PaperOreMaterialMapper {
    private static final Map<Material, OreKind> ORE_KINDS = Map.ofEntries(
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

    private PaperOreMaterialMapper() {}

    public static Optional<OreKind> oreKindFor(Material material) {
        return Optional.ofNullable(ORE_KINDS.get(material));
    }

    public static boolean isSupportedOre(Material material) {
        return ORE_KINDS.containsKey(material);
    }
}
