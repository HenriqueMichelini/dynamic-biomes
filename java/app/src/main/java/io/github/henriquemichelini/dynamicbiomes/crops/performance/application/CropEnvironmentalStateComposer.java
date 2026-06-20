package io.github.henriquemichelini.dynamicbiomes.crops.performance.application;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import lombok.NonNull;

public final class CropEnvironmentalStateComposer implements EnvironmentalStateComposer {

    private static final NormalizedEnvironmentalValue NEUTRAL_ENVIRONMENTAL_VALUE =
        new NormalizedEnvironmentalValue(0.50);

    private final BiomeResolver biomeResolver;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final SeasonProfileProvider seasonProfileProvider;

    public CropEnvironmentalStateComposer(
        @NonNull BiomeResolver biomeResolver,
        @NonNull CurrentSeasonQuery currentSeasonQuery,
        @NonNull SeasonProfileProvider seasonProfileProvider
    ) {
        this.biomeResolver = biomeResolver;
        this.currentSeasonQuery = currentSeasonQuery;
        this.seasonProfileProvider = seasonProfileProvider;
    }

    @Override
    public CropEnvironmentalState compose(BlockPosition position) {
        BiomeProfile biomeProfile = biomeResolver.resolve(position).profile();
        SeasonClimateAdjustment seasonClimate = currentSeasonClimate();

        NormalizedEnvironmentalValue windSpeed = NEUTRAL_ENVIRONMENTAL_VALUE;
        NormalizedEnvironmentalValue rainStrength = NEUTRAL_ENVIRONMENTAL_VALUE;

        NormalizedEnvironmentalValue humidity =
            seasonAdjustedEnvironmentalValue(
                biomeProfile.climate().humidity().normalized(),
                seasonClimate.humidity()
            );

        NormalizedEnvironmentalValue temperature =
            seasonAdjustedEnvironmentalValue(
                biomeProfile.climate().temperature().normalized(),
                seasonClimate.temperature()
            );

        NormalizedEnvironmentalValue solarIncidence =
            NEUTRAL_ENVIRONMENTAL_VALUE;

        NormalizedEnvironmentalValue soilFertility = environmentalValue(
            biomeProfile.fertility().normalized()
        );

        return new CropEnvironmentalState(
            windSpeed,
            rainStrength,
            humidity,
            temperature,
            solarIncidence,
            soilFertility
        );
    }

    private SeasonClimateAdjustment currentSeasonClimate() {
        SeasonProfile seasonProfile = seasonProfileProvider.profileFor(
            currentSeasonQuery.currentSeason()
        );

        return seasonProfile.climateAdjustment();
    }

    private static NormalizedEnvironmentalValue seasonAdjustedEnvironmentalValue(
        double biomeNormalizedValue,
        SeasonalAdjustment seasonAdjustment
    ) {
        double seasonAdjustmentFactor = 1.0 + seasonAdjustment.normalized();
        double adjustedValue = biomeNormalizedValue * seasonAdjustmentFactor;

        return environmentalValue(clamp01(adjustedValue));
    }

    private static NormalizedEnvironmentalValue environmentalValue(
        double normalizedValue
    ) {
        return new NormalizedEnvironmentalValue(normalizedValue);
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
