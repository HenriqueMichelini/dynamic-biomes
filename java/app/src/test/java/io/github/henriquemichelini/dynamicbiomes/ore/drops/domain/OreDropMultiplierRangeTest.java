package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OreDropMultiplierRangeTest {
    @Test
    void rejectsInvalidBounds() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(Double.NaN, 1.0)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(1.0, Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(Double.NEGATIVE_INFINITY, 1.0)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(1.0, Double.POSITIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(-0.1, 1.0)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(0.0, -0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierRange(1.1, 1.0))
        );
    }

    @Test
    void retainsValidInclusiveBounds() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);

        assertAll(
            () -> assertEquals(1.0, range.minimum()),
            () -> assertEquals(1.2, range.maximum())
        );
    }
}
