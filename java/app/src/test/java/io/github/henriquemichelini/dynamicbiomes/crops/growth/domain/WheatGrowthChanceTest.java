package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WheatGrowthChanceTest {
    @Test
    void rejectsChanceOutsideInclusiveUnitInterval() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChance(-0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChance(1.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChance(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChance(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChance(Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void retainsValidInclusiveChance() {
        assertAll(
            () -> assertEquals(0.0, new WheatGrowthChance(0.0).value()),
            () -> assertEquals(0.5, new WheatGrowthChance(0.5).value()),
            () -> assertEquals(1.0, new WheatGrowthChance(1.0).value())
        );
    }
}
