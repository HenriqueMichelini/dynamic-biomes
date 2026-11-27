package io.github.henriquemichelini.dynamicbiomes.crops.growth.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import lombok.NonNull;

public final class CropGrowthService {
    private final BiomeResolver biomeResolver;
    private final CropGrowthPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery;

    public CropGrowthService(
        @NonNull BiomeResolver biomeResolver,
        @NonNull CropGrowthPolicyProvider policyProvider,
        @NonNull CurrentSeasonQuery currentSeasonQuery,
        @NonNull BiFunction<BlockPosition, CropKind, CropPerformanceResult> cropPerformanceQuery
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.cropPerformanceQuery = cropPerformanceQuery;
    }

    public CropGrowthDecision decideNaturalGrowth(
        @NonNull BlockPosition position,
        @NonNull CropKind cropKind
    ) {
        return resolveGrowthStages(position, cropKind, OptionalInt.empty()) == 0
            ? CropGrowthDecision.CANCEL_GROWTH
            : CropGrowthDecision.ALLOW_GROWTH;
    }

    public int naturalGrowthStages(
        @NonNull BlockPosition position,
        @NonNull CropKind cropKind,
        int remainingStages
    ) {
        return resolveGrowthStages(
            position,
            cropKind,
            OptionalInt.of(remainingStages)
        );
    }

    private int resolveGrowthStages(
        BlockPosition position,
        CropKind cropKind,
        OptionalInt remainingStages
    ) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            CropGrowthPolicy policy = policyProvider.policyFor(
                biomeContext.biomeId(),
                cropKind
            );
            CropGrowthChance effectiveChance = policy.effectiveChanceFor(
                currentSeasonQuery.currentSeason()
            );
            CropPerformanceResult cropPerformance = cropPerformanceQuery.apply(
                position,
                cropKind
            );
            CropGrowthChance performanceAdjustedChance =
                effectiveChance.multipliedBy(cropPerformance.growthChanceFactor());

            if (policy.decide(performanceAdjustedChance) == CropGrowthDecision.CANCEL_GROWTH) {
                return 0;
            }
            return remainingStages.isPresent()
                ? policy.stagesForAllowedGrowth(
                    remainingStages.getAsInt(),
                    cropPerformance.growthSpeedFactor()
                )
                : 1;
        } catch (
            UnsupportedBiomeException
            | UnsupportedCropGrowthPolicyException e
        ) {
            return 1;
        }
    }
}
