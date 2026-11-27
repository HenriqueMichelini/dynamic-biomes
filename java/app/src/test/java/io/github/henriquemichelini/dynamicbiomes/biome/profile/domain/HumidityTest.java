package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HumidityTest {
    @Test
    void rejectsValuesOutsideNormalizedRange() {
        assertAll(
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Humidity(Double.NaN)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Humidity(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Humidity(Double.POSITIVE_INFINITY)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Humidity(-0.1)),
            () -> assertThrows(InvalidBiomeProfileException.class, () -> new Humidity(1.1))
        );
    }

    @Test
    void retainsInclusiveNormalizedBounds() {
        assertAll(
            () -> assertEquals(0.0, new Humidity(0.0).normalized()),
            () -> assertEquals(1.0, new Humidity(1.0).normalized())
        );
    }
}
