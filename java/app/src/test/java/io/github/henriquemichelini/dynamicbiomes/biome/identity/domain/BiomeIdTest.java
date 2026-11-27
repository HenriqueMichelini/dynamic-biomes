package io.github.henriquemichelini.dynamicbiomes.biome.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BiomeIdTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId(null)),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId("")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId(" ")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId("plains")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId(":plains")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId("minecraft:")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId("Minecraft:plains")),
            () -> assertThrows(InvalidBiomeIdException.class, () -> new BiomeId("minecraft:bad path"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        BiomeId biomeId = new BiomeId("minecraft:windswept_hills");

        assertEquals("minecraft:windswept_hills", biomeId.value());
    }
}
