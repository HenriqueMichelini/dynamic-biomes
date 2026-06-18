package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import org.junit.jupiter.api.Test;

class CropYieldBiomeFactorCalculatorTest {

    private final CropYieldBiomeFactorCalculator calculator =
        new CropYieldBiomeFactorCalculator();

    @Test
    void derivesNeutralBiomeYieldFactorFromMidpointFertility() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(new Fertility(0.50));

        assertEquals(1.00, factor.factor());
    }

    @Test
    void derivesMaximumBiomeYieldFactorFromMaximumFertility() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(new Fertility(1.00));

        assertEquals(1.20, factor.factor(), 0.000_000_001);
    }

    @Test
    void derivesMinimumBiomeYieldFactorFromMinimumFertility() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(new Fertility(0.00));

        assertEquals(0.80, factor.factor());
    }

    @Test
    void derivesPositiveIntermediateBiomeYieldFactor() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(new Fertility(0.75));

        assertEquals(1.10, factor.factor(), 0.000_000_001);
    }

    @Test
    void derivesNegativeIntermediateBiomeYieldFactor() {
        CropYieldEnvironmentalFactor factor = calculator.calculate(new Fertility(0.25));

        assertEquals(0.90, factor.factor(), 0.000_000_001);
    }

    @Test
    void rejectsNullFertility() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
    }
}
