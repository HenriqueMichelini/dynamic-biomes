package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OreDropPolicyTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");

    @Test
    void returnsMultiplierRangeForTypedOreKind() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);
        OreDropPolicy policy = new OreDropPolicy(FOREST, Map.of(IRON_ORE, range));

        assertEquals(FOREST, policy.biomeId());
        assertEquals(range, policy.multiplierRangeFor(IRON_ORE));
    }

    @Test
    void failsClearlyWhenOreKindIsMissing() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(IRON_ORE, new OreDropMultiplierRange(1.0, 1.2))
        );
        OreKind missingOre = new OreKind("minecraft:gold_ore");

        assertThrows(
            IllegalArgumentException.class,
            () -> policy.multiplierRangeFor(missingOre)
        );
    }
}
