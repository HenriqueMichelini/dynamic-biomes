package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TemperatureTest {
    @Test
    void rejectsValuesOutsideNormalizedRange() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new Temperature(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new Temperature(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new Temperature(Double.POSITIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new Temperature(-0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new Temperature(1.1))
        );
    }

    @Test
    void retainsInclusiveNormalizedBounds() {
        assertAll(
            () -> assertEquals(0.0, new Temperature(0.0).normalized()),
            () -> assertEquals(1.0, new Temperature(1.0).normalized())
        );
    }
}
