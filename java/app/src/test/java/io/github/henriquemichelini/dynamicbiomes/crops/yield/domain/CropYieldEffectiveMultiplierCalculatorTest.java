package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropYieldEffectiveMultiplierCalculatorTest {

    private final CropYieldEffectiveMultiplierCalculator calculator =
        new CropYieldEffectiveMultiplierCalculator();

    @Test
    void composesNeutralOptionBMultiplier() {
        double multiplier = calculator.calculate(
            1.00,
            new CropYieldSeasonalFactor(1.00),
            new CropYieldEnvironmentalFactor(1.00)
        );

        assertEquals(1.00, multiplier);
    }

    @Test
    void composesSelectedBaseSeasonalAndEnvironmentalFactors() {
        double multiplier = calculator.calculate(
            1.25,
            new CropYieldSeasonalFactor(1.10),
            new CropYieldEnvironmentalFactor(0.96)
        );

        assertEquals(1.32, multiplier, 0.000_000_001);
    }

    @Test
    void composesLowerBaseWithPositiveEnvironmentalFactor() {
        double multiplier = calculator.calculate(
            0.80,
            new CropYieldSeasonalFactor(1.00),
            new CropYieldEnvironmentalFactor(1.20)
        );

        assertEquals(0.96, multiplier, 0.000_000_001);
    }

    @Test
    void preservesZeroSelectedBaseMultiplier() {
        double multiplier = calculator.calculate(
            0.00,
            new CropYieldSeasonalFactor(1.00),
            new CropYieldEnvironmentalFactor(1.20)
        );

        assertEquals(0.00, multiplier);
    }

    @Test
    void preservesZeroEnvironmentalFactor() {
        double multiplier = calculator.calculate(
            1.00,
            new CropYieldSeasonalFactor(1.20),
            new CropYieldEnvironmentalFactor(0.00)
        );

        assertEquals(0.00, multiplier);
    }

    @Test
    void rejectsInvalidSelectedBaseMultiplier() {
        CropYieldSeasonalFactor seasonalFactor = new CropYieldSeasonalFactor(1.00);
        CropYieldEnvironmentalFactor environmentalFactor =
            new CropYieldEnvironmentalFactor(1.00);

        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.calculate(-0.01, seasonalFactor, environmentalFactor)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.calculate(Double.NaN, seasonalFactor, environmentalFactor)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.calculate(
                Double.POSITIVE_INFINITY,
                seasonalFactor,
                environmentalFactor
            )
        );
    }

    @Test
    void rejectsNullFactors() {
        CropYieldSeasonalFactor seasonalFactor = new CropYieldSeasonalFactor(1.00);
        CropYieldEnvironmentalFactor environmentalFactor =
            new CropYieldEnvironmentalFactor(1.00);

        assertThrows(
            NullPointerException.class,
            () -> calculator.calculate(1.00, null, environmentalFactor)
        );
        assertThrows(
            NullPointerException.class,
            () -> calculator.calculate(1.00, seasonalFactor, null)
        );
    }
}
