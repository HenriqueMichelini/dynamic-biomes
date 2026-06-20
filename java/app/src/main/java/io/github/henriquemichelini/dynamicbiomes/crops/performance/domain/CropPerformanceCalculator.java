package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import java.util.OptionalDouble;
import lombok.NonNull;

public final class CropPerformanceCalculator {

    private static final int SCORED_ENVIRONMENTAL_VARIABLES = 6;

    public CropPerformanceResult calculate(
        @NonNull CropPerformanceProfile profile,
        @NonNull CropEnvironmentalState state
    ) {
        double overallScore = overallScore(profile, state);

        double growthSpeedFactor = overallScore;
        double growthChanceFactor = overallScore;
        double harvestQuantityFactor = overallScore;

        return new CropPerformanceResult(
            OptionalDouble.of(overallScore),
            growthSpeedFactor,
            growthChanceFactor,
            harvestQuantityFactor
        );
    }

    private static double overallScore(
        CropPerformanceProfile profile,
        CropEnvironmentalState state
    ) {
        double windSpeedMatch = match(
            profile.preferredWindSpeed(),
            state.windSpeed()
        );
        double rainStrengthMatch = match(
            profile.preferredRainStrength(),
            state.rainStrength()
        );
        double humidityMatch = match(
            profile.preferredHumidity(),
            state.humidity()
        );
        double temperatureMatch = match(
            profile.preferredTemperature(),
            state.temperature()
        );
        double solarIncidenceMatch = match(
            profile.preferredSolarIncidence(),
            state.solarIncidence()
        );
        double soilFertilityMatch = match(
            profile.preferredSoilFertility(),
            state.soilFertility()
        );

        return (
            windSpeedMatch +
                rainStrengthMatch +
                humidityMatch +
                temperatureMatch +
                solarIncidenceMatch +
                soilFertilityMatch
        ) / SCORED_ENVIRONMENTAL_VARIABLES;
    }

    private static double match(
        NormalizedEnvironmentalValue preferred,
        NormalizedEnvironmentalValue actual
    ) {
        return 1.0 - Math.abs(preferred.normalized() - actual.normalized());
    }
}
