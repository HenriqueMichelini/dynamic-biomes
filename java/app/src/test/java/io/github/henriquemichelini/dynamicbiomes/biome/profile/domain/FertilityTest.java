package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FertilityTest {
    @Test
    void rejectsValuesOutsideNormalizedRange() {
        assertAll(
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Fertility(Double.NaN)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Fertility(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Fertility(Double.POSITIVE_INFINITY)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Fertility(-0.1)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Fertility(1.1))
        );
    }

    @Test
    void retainsInclusiveNormalizedBounds() {
        assertAll(
            () -> assertEquals(0.0, new Fertility(0.0).normalized()),
            () -> assertEquals(1.0, new Fertility(1.0).normalized())
        );
    }
}
