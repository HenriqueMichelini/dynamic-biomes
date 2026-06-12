package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OreDropMultiplierCalculatorTest {
    @Test
    void returnsExactMultiplierForEqualBounds() {
        OreDropMultiplierCalculator calculator = new OreDropMultiplierCalculator(
            () -> {
                throw new AssertionError("Variation is unnecessary for an exact range");
            }
        );

        double multiplier = calculator.calculate(new OreDropMultiplierRange(1.2, 1.2));

        assertEquals(1.2, multiplier);
    }

    @Test
    void returnsMultiplierWithinInclusiveRange() {
        OreDropMultiplierCalculator calculator = new OreDropMultiplierCalculator(() -> 0.37);
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);

        double multiplier = calculator.calculate(range);

        assertTrue(multiplier >= range.minimum());
        assertTrue(multiplier <= range.maximum());
    }

    @Test
    void includesBothRangeBounds() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);

        assertAll(
            () -> assertEquals(1.0, new OreDropMultiplierCalculator(() -> 0.0).calculate(range)),
            () -> assertEquals(1.2, new OreDropMultiplierCalculator(() -> 1.0).calculate(range))
        );
    }

    @Test
    void isDeterministicForDeterministicVariation() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);

        double first = new OreDropMultiplierCalculator(() -> 0.5).calculate(range);
        double second = new OreDropMultiplierCalculator(() -> 0.5).calculate(range);

        assertEquals(first, second);
        assertEquals(1.1, first);
    }

    @Test
    void rejectsVariationOutsideUnitInterval() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);

        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierCalculator(() -> -0.1).calculate(range)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierCalculator(() -> 1.1).calculate(range)),
            () -> assertThrows(IllegalArgumentException.class, () -> new OreDropMultiplierCalculator(() -> Double.NaN).calculate(range))
        );
    }
}
