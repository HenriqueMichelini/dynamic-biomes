package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropGrowthChanceTest {
    @Test
    void rejectsChanceOutsideInclusiveUnitInterval() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new CropGrowthChance(-0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropGrowthChance(1.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropGrowthChance(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropGrowthChance(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropGrowthChance(Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void retainsValidInclusiveChance() {
        assertAll(
            () -> assertEquals(0.0, new CropGrowthChance(0.0).value()),
            () -> assertEquals(0.5, new CropGrowthChance(0.5).value()),
            () -> assertEquals(1.0, new CropGrowthChance(1.0).value())
        );
    }
}
