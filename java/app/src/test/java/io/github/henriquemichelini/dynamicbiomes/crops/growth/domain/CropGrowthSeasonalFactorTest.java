package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropGrowthSeasonalFactorTest {
    @Test
    void acceptsNonNegativeFiniteFactors() {
        assertAll(
            () -> assertEquals(0.0, new CropGrowthSeasonalFactor(0.0).factor()),
            () -> assertEquals(1.0, new CropGrowthSeasonalFactor(1.0).factor()),
            () -> assertEquals(1.5, new CropGrowthSeasonalFactor(1.5).factor())
        );
    }

    @Test
    void rejectsNegativeOrNonFiniteFactors() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthSeasonalFactor(-0.1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthSeasonalFactor(Double.NaN)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthSeasonalFactor(Double.NEGATIVE_INFINITY)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthSeasonalFactor(Double.POSITIVE_INFINITY)
            )
        );
    }
}
