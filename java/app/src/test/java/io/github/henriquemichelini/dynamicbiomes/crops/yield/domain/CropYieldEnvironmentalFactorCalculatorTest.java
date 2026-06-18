package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropYieldEnvironmentalFactorCalculatorTest {

    private final CropYieldEnvironmentalFactorCalculator calculator =
        new CropYieldEnvironmentalFactorCalculator();

    @Test
    void combinesBiomeAndClimateYieldFactors() {
        CropYieldEnvironmentalFactor environmentalFactor = calculator.calculate(
            new CropYieldEnvironmentalFactor(1.20),
            new CropYieldEnvironmentalFactor(0.80)
        );

        assertEquals(0.96, environmentalFactor.factor(), 0.000_000_001);
    }

    @Test
    void combinesNeutralFactors() {
        CropYieldEnvironmentalFactor environmentalFactor = calculator.calculate(
            new CropYieldEnvironmentalFactor(1.00),
            new CropYieldEnvironmentalFactor(1.00)
        );

        assertEquals(1.00, environmentalFactor.factor());
    }

    @Test
    void preservesZeroFactor() {
        CropYieldEnvironmentalFactor environmentalFactor = calculator.calculate(
            new CropYieldEnvironmentalFactor(0.00),
            new CropYieldEnvironmentalFactor(1.00)
        );

        assertEquals(0.00, environmentalFactor.factor());
    }

    @Test
    void preservesHighPositiveProduct() {
        CropYieldEnvironmentalFactor environmentalFactor = calculator.calculate(
            new CropYieldEnvironmentalFactor(100.0),
            new CropYieldEnvironmentalFactor(25.0)
        );

        assertEquals(2500.0, environmentalFactor.factor());
    }

    @Test
    void rejectsNullFactors() {
        CropYieldEnvironmentalFactor neutralFactor =
            new CropYieldEnvironmentalFactor(1.0);

        assertThrows(
            NullPointerException.class,
            () -> calculator.calculate(null, neutralFactor)
        );
        assertThrows(
            NullPointerException.class,
            () -> calculator.calculate(neutralFactor, null)
        );
    }
}
