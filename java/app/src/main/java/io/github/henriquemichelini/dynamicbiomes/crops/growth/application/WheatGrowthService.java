package io.github.henriquemichelini.dynamicbiomes.crops.growth.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedWheatGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class WheatGrowthService {
    private final BiomeResolver biomeResolver;
    private final WheatGrowthChancePolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;

    public WheatGrowthService(
        BiomeResolver biomeResolver,
        WheatGrowthChancePolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
    }

    public WheatGrowthDecision decideNaturalWheatGrowth(BlockPosition position) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            WheatGrowthChancePolicy policy = policyProvider.policyFor(
                biomeContext.biomeId()
            );
            return policy.decide(currentSeasonQuery.currentSeason());
        } catch (
            UnsupportedBiomeException
            | UnsupportedWheatGrowthPolicyException e
        ) {
            return WheatGrowthDecision.ALLOW_GROWTH;
        }
    }
}
