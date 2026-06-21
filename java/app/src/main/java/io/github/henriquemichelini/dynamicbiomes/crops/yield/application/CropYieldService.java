package io.github.henriquemichelini.dynamicbiomes.crops.yield.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.function.BiFunction;
import lombok.NonNull;

public final class CropYieldService {
    private final BiomeResolver biomeResolver;
    private final CropYieldPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery;
    private final CropYieldMultiplierCalculator multiplierCalculator;
    private final CropYieldQuantityCalculator quantityCalculator;

    public CropYieldService(
        @NonNull BiomeResolver biomeResolver,
        @NonNull CropYieldPolicyProvider policyProvider,
        @NonNull CurrentSeasonQuery currentSeasonQuery,
        @NonNull BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery,
        @NonNull CropYieldMultiplierCalculator multiplierCalculator,
        @NonNull CropYieldQuantityCalculator quantityCalculator
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.cropPerformanceQuery = cropPerformanceQuery;
        this.multiplierCalculator = multiplierCalculator;
        this.quantityCalculator = quantityCalculator;
    }

    public int calculateProduceQuantity(
        @NonNull BlockPosition position,
        @NonNull CropKind cropKind,
        int vanillaProduceQuantity
    ) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            CropYieldPolicy policy = policyProvider.policyFor(biomeContext.biomeId());
            double selectedMultiplier = multiplierCalculator.calculate(
                policy.multiplierRangeFor(cropKind)
            );
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            CropYieldSeasonalFactor seasonalFactor = new CropYieldSeasonalFactor(
                policy.seasonalFactorFor(cropKind, currentSeason)
            );
            CropPerformanceResult cropPerformance = cropPerformanceQuery.apply(
                position,
                cropKind
            );
            double effectiveMultiplier = calculateEffectiveMultiplier(
                selectedMultiplier,
                seasonalFactor,
                cropPerformance
            );

            return quantityCalculator.calculate(
                vanillaProduceQuantity,
                effectiveMultiplier
            );
        } catch (
            UnsupportedBiomeException
            | UnsupportedCropYieldPolicyException e
        ) {
            return vanillaProduceQuantity;
        }
    }

    private double calculateEffectiveMultiplier(
        double selectedMultiplier,
        CropYieldSeasonalFactor seasonalFactor,
        CropPerformanceResult cropPerformance
    ) {
        return selectedMultiplier
            * seasonalFactor.factor()
            * cropPerformance.harvestQuantityFactor();
    }

}
