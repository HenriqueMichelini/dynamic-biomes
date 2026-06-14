package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class OreDropService {

    private final OreOriginTrackingService originTracking;
    private final BiomeResolver biomeResolver;
    private final OreDropPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final OreDropMultiplierCalculator multiplierCalculator;
    private final OreDropQuantityCalculator quantityCalculator;

    public OreDropService(
        OreOriginTrackingService originTracking,
        BiomeResolver biomeResolver,
        OreDropPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        OreDropMultiplierCalculator multiplierCalculator,
        OreDropQuantityCalculator quantityCalculator
    ) {
        this.originTracking = originTracking;
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.multiplierCalculator = multiplierCalculator;
        this.quantityCalculator = quantityCalculator;
    }

    public int calculateDrops(
        BlockPosition position,
        OreKind oreKind,
        int vanillaFortuneQuantity
    ) {
        if (!originTracking.isEligibleForBiomeMultiplier(position)) {
            return vanillaFortuneQuantity;
        }

        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            OreDropPolicy policy = policyProvider.policyFor(biomeContext.biomeId());
            double selectedMultiplier = multiplierCalculator.calculate(
                policy.multiplierRangeFor(oreKind)
            );
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            double seasonalFactor = policy.seasonalMultiplierFactorFor(
                oreKind,
                currentSeason
            );

            return quantityCalculator.calculate(
                vanillaFortuneQuantity,
                selectedMultiplier * seasonalFactor
            );
        } catch (
            UnsupportedBiomeException
            | UnsupportedOreDropConfigurationException e
        ) {
            return vanillaFortuneQuantity;
        }
    }
}
