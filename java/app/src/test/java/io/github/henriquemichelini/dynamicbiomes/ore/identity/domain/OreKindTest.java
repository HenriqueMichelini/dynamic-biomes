package io.github.henriquemichelini.dynamicbiomes.ore.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OreKindTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind(null)),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind("")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind(" ")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind("iron_ore")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind(":iron_ore")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind("minecraft:")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind("Minecraft:iron_ore")),
            () -> assertThrows(InvalidOreKindException.class, () -> new OreKind("minecraft:bad ore"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        OreKind oreKind = new OreKind("minecraft:deepslate_iron_ore");

        assertEquals("minecraft:deepslate_iron_ore", oreKind.value());
    }
}
