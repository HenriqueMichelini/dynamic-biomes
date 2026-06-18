package io.github.henriquemichelini.dynamicbiomes.crops.yield.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class CropYieldService {
    private final BiomeResolver biomeResolver;
    private final CropYieldPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final CropYieldMultiplierCalculator multiplierCalculator;
    private final CropYieldQuantityCalculator quantityCalculator;

    public CropYieldService(
        BiomeResolver biomeResolver,
        CropYieldPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        CropYieldMultiplierCalculator multiplierCalculator,
        CropYieldQuantityCalculator quantityCalculator
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.multiplierCalculator = multiplierCalculator;
        this.quantityCalculator = quantityCalculator;
    }

    public int calculateProduceQuantity(
        BlockPosition position,
        CropKind cropKind,
        int vanillaProduceQuantity
    ) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            CropYieldPolicy policy = policyProvider.policyFor(biomeContext.biomeId());
            double selectedMultiplier = multiplierCalculator.calculate(
                policy.multiplierRangeFor(cropKind)
            );
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            double seasonalFactor = policy.seasonalFactorFor(cropKind, currentSeason);

            return quantityCalculator.calculate(
                vanillaProduceQuantity,
                selectedMultiplier * seasonalFactor
            );
        } catch (
            UnsupportedBiomeException
            | UnsupportedCropYieldPolicyException e
        ) {
            return vanillaProduceQuantity;
        }
    }
}
