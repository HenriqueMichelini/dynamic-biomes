package io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeasonIdTest {
    @Test
    void rejectsInvalidIdentifiers() {
        assertAll(
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId(null)),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId("")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId(" ")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId("spring")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId(":spring")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId("dynamicbiomes:")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId("DynamicBiomes:spring")),
            () -> assertThrows(InvalidSeasonIdException.class, () -> new SeasonId("dynamicbiomes:early spring"))
        );
    }

    @Test
    void retainsValidNamespacedIdentifier() {
        SeasonId seasonId = new SeasonId("dynamicbiomes:early_spring");

        assertEquals("dynamicbiomes:early_spring", seasonId.value());
    }
}
