package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class OreDropService {
    private final OreOriginTrackingService originTracking;
    private final OreDropEnvironmentQueryService environmentQuery;
    private final OreDropPolicyProvider policyProvider;
    private final OreDropMultiplierCalculator multiplierCalculator;
    private final OreDropQuantityCalculator quantityCalculator;

    public OreDropService(
        OreOriginTrackingService originTracking,
        OreDropEnvironmentQueryService environmentQuery,
        OreDropPolicyProvider policyProvider,
        OreDropMultiplierCalculator multiplierCalculator,
        OreDropQuantityCalculator quantityCalculator
    ) {
        this.originTracking = originTracking;
        this.environmentQuery = environmentQuery;
        this.policyProvider = policyProvider;
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

        OreDropEnvironmentContext environmentContext = environmentQuery.resolve(position);
        BiomeContext biomeContext = environmentContext.biomeContext();
        OreDropPolicy policy = policyProvider.policyFor(biomeContext.biomeId());
        double selectedMultiplier = multiplierCalculator.calculate(
            policy.multiplierRangeFor(oreKind)
        );

        return quantityCalculator.calculate(
            vanillaFortuneQuantity,
            selectedMultiplier
        );
    }
}
