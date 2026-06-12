package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MineralRichnessTest {
    @Test
    void rejectsValuesOutsideNormalizedRange() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new MineralRichness(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new MineralRichness(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new MineralRichness(Double.POSITIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new MineralRichness(-0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new MineralRichness(1.1))
        );
    }

    @Test
    void retainsInclusiveNormalizedBounds() {
        assertAll(
            () -> assertEquals(0.0, new MineralRichness(0.0).normalized()),
            () -> assertEquals(1.0, new MineralRichness(1.0).normalized())
        );
    }
}
