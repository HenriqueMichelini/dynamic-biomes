package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import java.util.Objects;

public record CropEnvironmentalState(
    NormalizedEnvironmentalValue windSpeed,
    NormalizedEnvironmentalValue rainStrength,
    NormalizedEnvironmentalValue humidity,
    NormalizedEnvironmentalValue temperature,
    NormalizedEnvironmentalValue solarIncidence,
    NormalizedEnvironmentalValue soilFertility
) {
    public CropEnvironmentalState {
        Objects.requireNonNull(windSpeed, "windSpeed");
        Objects.requireNonNull(rainStrength, "rainStrength");
        Objects.requireNonNull(humidity, "humidity");
        Objects.requireNonNull(temperature, "temperature");
        Objects.requireNonNull(solarIncidence, "solarIncidence");
        Objects.requireNonNull(soilFertility, "soilFertility");
    }
}
