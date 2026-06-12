package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OreDropQuantityCalculatorTest {
    @Test
    void returnsZeroForZeroVanillaQuantity() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(
            () -> {
                throw new AssertionError("Variation is unnecessary for zero quantity");
            }
        );

        assertEquals(0, calculator.calculate(0, 1.5));
    }

    @Test
    void returnsVanillaQuantityForNeutralMultiplier() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(
            () -> {
                throw new AssertionError("Variation is unnecessary for a neutral multiplier");
            }
        );

        assertEquals(3, calculator.calculate(3, 1.0));
    }

    @Test
    void returnsExactWholeNumberResult() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(
            () -> {
                throw new AssertionError("Variation is unnecessary for a whole-number result");
            }
        );

        assertEquals(6, calculator.calculate(3, 2.0));
    }

    @Test
    void floorsFractionalResultWhenExtraItemIsNotAwarded() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.5);

        assertEquals(4, calculator.calculate(3, 1.5));
    }

    @Test
    void awardsExtraItemWhenVariationFallsWithinFractionalRemainder() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.49);

        assertEquals(5, calculator.calculate(3, 1.5));
    }

    @Test
    void fractionalExtraIsDeterministicForDeterministicVariation() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.25);

        assertAll(
            () -> assertEquals(5, calculator.calculate(3, 1.5)),
            () -> assertEquals(5, calculator.calculate(3, 1.5))
        );
    }

    @Test
    void rejectsNegativeVanillaQuantity() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.0);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(-1, 1.0));
    }

    @Test
    void rejectsInvalidMultiplier() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.0);

        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> calculator.calculate(3, -0.1)),
            () -> assertThrows(IllegalArgumentException.class, () -> calculator.calculate(3, Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> calculator.calculate(3, Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void rejectsMultipliedQuantityBeyondSupportedIntegerRange() {
        OreDropQuantityCalculator calculator = new OreDropQuantityCalculator(() -> 0.0);

        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.calculate(Integer.MAX_VALUE, 2.0)
        );
    }

    @Test
    void rejectsVariationOutsideUnitInterval() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new OreDropQuantityCalculator(() -> -0.1).calculate(3, 1.5)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new OreDropQuantityCalculator(() -> 1.1).calculate(3, 1.5)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new OreDropQuantityCalculator(() -> Double.NaN).calculate(3, 1.5)
            )
        );
    }
}
