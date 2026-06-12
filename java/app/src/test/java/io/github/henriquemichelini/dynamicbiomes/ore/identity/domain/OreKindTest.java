package io.github.henriquemichelini.dynamicbiomes.ore.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OreKindTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind("")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind(" ")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind("iron_ore")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind(":iron_ore")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind("minecraft:")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind("Minecraft:iron_ore")),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreKind("minecraft:bad ore"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        OreKind oreKind = new OreKind("minecraft:deepslate_iron_ore");

        assertEquals("minecraft:deepslate_iron_ore", oreKind.value());
    }
}
