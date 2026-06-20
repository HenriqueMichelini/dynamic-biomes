package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import lombok.NonNull;

public record CropPerformanceProfile(
    @NonNull CropKind cropKind,
    @NonNull NormalizedEnvironmentalValue preferredWindSpeed,
    @NonNull NormalizedEnvironmentalValue preferredRainStrength,
    @NonNull NormalizedEnvironmentalValue preferredHumidity,
    @NonNull NormalizedEnvironmentalValue preferredTemperature,
    @NonNull NormalizedEnvironmentalValue preferredSolarIncidence,
    @NonNull NormalizedEnvironmentalValue preferredSoilFertility
) {
    public CropPerformanceProfile {
    }
}
