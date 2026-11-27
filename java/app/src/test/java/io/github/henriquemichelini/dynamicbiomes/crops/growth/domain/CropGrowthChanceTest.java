package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropGrowthChanceTest {
    @Test
    void rejectsChanceOutsideInclusiveUnitInterval() {
        assertAll(
            () -> assertThrows(InvalidCropGrowthPolicyException.class, () -> new CropGrowthChance(-0.1)),
            () -> assertThrows(InvalidCropGrowthPolicyException.class, () -> new CropGrowthChance(1.1)),
            () -> assertThrows(InvalidCropGrowthPolicyException.class, () -> new CropGrowthChance(Double.NaN)),
            () -> assertThrows(InvalidCropGrowthPolicyException.class, () -> new CropGrowthChance(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(InvalidCropGrowthPolicyException.class, () -> new CropGrowthChance(Double.POSITIVE_INFINITY))
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

    @Test
    void appliesFiniteNonNegativeFactorsAndCapsTheResult() {
        CropGrowthChance chance = new CropGrowthChance(0.5);

        assertAll(
            () -> assertEquals(0.25, chance.multipliedBy(0.5).value()),
            () -> assertEquals(1.0, chance.multipliedBy(3.0).value()),
            () -> assertThrows(IllegalArgumentException.class, () -> chance.multipliedBy(-0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> chance.multipliedBy(Double.POSITIVE_INFINITY))
        );
    }
}
