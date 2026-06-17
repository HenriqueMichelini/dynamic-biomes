package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WheatGrowthSeasonalFactorTest {
    @Test
    void acceptsNonNegativeFiniteFactors() {
        assertAll(
            () -> assertEquals(0.0, new WheatGrowthSeasonalFactor(0.0).factor()),
            () -> assertEquals(1.0, new WheatGrowthSeasonalFactor(1.0).factor()),
            () -> assertEquals(1.5, new WheatGrowthSeasonalFactor(1.5).factor())
        );
    }

    @Test
    void rejectsNegativeOrNonFiniteFactors() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new WheatGrowthSeasonalFactor(-0.1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new WheatGrowthSeasonalFactor(Double.NaN)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new WheatGrowthSeasonalFactor(Double.NEGATIVE_INFINITY)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new WheatGrowthSeasonalFactor(Double.POSITIVE_INFINITY)
            )
        );
    }
}
