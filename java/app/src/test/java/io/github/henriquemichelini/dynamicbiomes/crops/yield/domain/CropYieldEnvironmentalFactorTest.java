package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropYieldEnvironmentalFactorTest {

    @Test
    void acceptsZeroAndPositiveFactors() {
        assertEquals(0.0, new CropYieldEnvironmentalFactor(0.0).factor());
        assertEquals(1.0, new CropYieldEnvironmentalFactor(1.0).factor());
        assertEquals(25.0, new CropYieldEnvironmentalFactor(25.0).factor());
    }

    @Test
    void rejectsNegativeFactors() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CropYieldEnvironmentalFactor(-0.01)
        );
    }

    @Test
    void rejectsNonFiniteFactors() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CropYieldEnvironmentalFactor(Double.NaN)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new CropYieldEnvironmentalFactor(Double.POSITIVE_INFINITY)
        );
    }
}
