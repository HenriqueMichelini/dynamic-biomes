package io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeasonIdTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId(null)),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId("")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId(" ")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId("spring")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId(":spring")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId("dynamicbiomes:")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId("DynamicBiomes:spring")),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonId("dynamicbiomes:early spring"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        SeasonId seasonId = new SeasonId("dynamicbiomes:early_spring");

        assertEquals("dynamicbiomes:early_spring", seasonId.value());
    }
}
