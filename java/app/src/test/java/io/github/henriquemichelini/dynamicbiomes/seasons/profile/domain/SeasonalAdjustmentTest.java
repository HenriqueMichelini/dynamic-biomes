package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeasonalAdjustmentTest {
    @Test
    void rejectsNonFiniteAndOutOfRangeValues() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonalAdjustment(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonalAdjustment(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonalAdjustment(Double.POSITIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonalAdjustment(-1.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new SeasonalAdjustment(1.1))
        );
    }

    @Test
    void retainsInclusiveAdjustmentBoundsAndNeutralValue() {
        assertAll(
            () -> assertEquals(-1.0, new SeasonalAdjustment(-1.0).normalized()),
            () -> assertEquals(0.0, new SeasonalAdjustment(0.0).normalized()),
            () -> assertEquals(1.0, new SeasonalAdjustment(1.0).normalized())
        );
    }
}
