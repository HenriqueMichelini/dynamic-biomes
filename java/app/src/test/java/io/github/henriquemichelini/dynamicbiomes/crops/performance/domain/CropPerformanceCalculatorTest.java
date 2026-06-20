package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import org.junit.jupiter.api.Test;

class CropPerformanceCalculatorTest {

    private final CropPerformanceCalculator calculator = new CropPerformanceCalculator();

    @Test
    void exactPreferenceMatchProducesNeutralPositivePerformanceFactors() {
        CropPerformanceProfile profile = profile(0.2, 0.3, 0.4, 0.5, 0.6, 0.7);
        CropEnvironmentalState state = state(0.2, 0.3, 0.4, 0.5, 0.6, 0.7);

        CropPerformanceResult result = calculator.calculate(profile, state);

        assertAll(
            () -> assertEquals(1.0, result.overallScore().orElseThrow()),
            () -> assertEquals(1.0, result.growthSpeedFactor()),
            () -> assertEquals(1.0, result.growthChanceFactor()),
            () -> assertEquals(1.0, result.harvestQuantityFactor())
        );
    }

    @Test
    void poorEnvironmentalMatchReducesPerformanceFactors() {
        CropPerformanceProfile profile = profile(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        CropEnvironmentalState state = state(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

        CropPerformanceResult result = calculator.calculate(profile, state);

        assertAll(
            () -> assertEquals(0.0, result.overallScore().orElseThrow()),
            () -> assertEquals(0.0, result.growthSpeedFactor()),
            () -> assertEquals(0.0, result.growthChanceFactor()),
            () -> assertEquals(0.0, result.harvestQuantityFactor())
        );
    }

    private static CropPerformanceProfile profile(
        double windSpeed,
        double rainStrength,
        double humidity,
        double temperature,
        double solarIncidence,
        double soilFertility
    ) {
        return new CropPerformanceProfile(
            CropKind.WHEAT,
            value(windSpeed),
            value(rainStrength),
            value(humidity),
            value(temperature),
            value(solarIncidence),
            value(soilFertility)
        );
    }

    private static CropEnvironmentalState state(
        double windSpeed,
        double rainStrength,
        double humidity,
        double temperature,
        double solarIncidence,
        double soilFertility
    ) {
        return new CropEnvironmentalState(
            value(windSpeed),
            value(rainStrength),
            value(humidity),
            value(temperature),
            value(solarIncidence),
            value(soilFertility)
        );
    }

    private static NormalizedEnvironmentalValue value(double normalized) {
        return new NormalizedEnvironmentalValue(normalized);
    }
}
