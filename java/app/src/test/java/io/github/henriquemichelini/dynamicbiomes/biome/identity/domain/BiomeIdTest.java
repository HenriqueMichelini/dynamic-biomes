package io.github.henriquemichelini.dynamicbiomes.biome.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BiomeIdTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId("")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId(" ")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId("plains")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId(":plains")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId("minecraft:")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId("Minecraft:plains")),
            () -> assertThrows(IllegalArgumentException.class, () -> new BiomeId("minecraft:bad path"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        BiomeId biomeId = new BiomeId("minecraft:windswept_hills");

        assertEquals("minecraft:windswept_hills", biomeId.value());
    }
}
