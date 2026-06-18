package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import org.junit.jupiter.api.Test;

class CropYieldClimateFactorCalculatorTest {

    private final CropYieldClimateFactorCalculator calculator =
        new CropYieldClimateFactorCalculator();

    @Test
    void derivesNeutralClimateYieldFactorFromNeutralTemperatureAndHumidity() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(adjustment(0.00, 0.00));

        assertEquals(1.00, factor.factor());
    }

    @Test
    void derivesMaximumClimateYieldFactorFromMaximumTemperatureAndHumidity() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(adjustment(1.00, 1.00));

        assertEquals(1.15, factor.factor(), 0.000_000_001);
    }

    @Test
    void derivesMinimumClimateYieldFactorFromMinimumTemperatureAndHumidity() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(adjustment(-1.00, -1.00));

        assertEquals(0.85, factor.factor(), 0.000_000_001);
    }

    @Test
    void derivesNeutralClimateYieldFactorFromOppositeMaximumAdjustments() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(adjustment(1.00, -1.00));

        assertEquals(1.00, factor.factor());
    }

    @Test
    void derivesNeutralClimateYieldFactorFromOppositeIntermediateAdjustments() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(adjustment(0.50, -0.50));

        assertEquals(1.00, factor.factor());
    }

    @Test
    void rejectsNullClimateAdjustment() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
    }

    private static SeasonClimateAdjustment adjustment(double temperature, double humidity) {
        return new SeasonClimateAdjustment(
            new SeasonalAdjustment(temperature),
            new SeasonalAdjustment(humidity)
        );
    }
}
