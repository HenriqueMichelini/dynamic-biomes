package io.github.henriquemichelini.dynamicbiomes.crops.growth.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class CropGrowthService {
    private final BiomeResolver biomeResolver;
    private final CropGrowthPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;

    public CropGrowthService(
        BiomeResolver biomeResolver,
        CropGrowthPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
    }

    public CropGrowthDecision decideNaturalGrowth(
        BlockPosition position,
        CropKind cropKind
    ) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            CropGrowthPolicy policy = policyProvider.policyFor(
                biomeContext.biomeId(),
                cropKind
            );
            return policy.decide(currentSeasonQuery.currentSeason());
        } catch (
            UnsupportedBiomeException
            | UnsupportedCropGrowthPolicyException e
        ) {
            return CropGrowthDecision.ALLOW_GROWTH;
        }
    }
}
