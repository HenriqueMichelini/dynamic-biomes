package io.github.henriquemichelini.dynamicbiomes.trees.growth.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.UnsupportedTreeGrowthPolicyException;
import lombok.NonNull;

public final class TreeGrowthService {
    private final BiomeResolver biomeResolver;
    private final TreeGrowthPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;

    public TreeGrowthService(
        @NonNull BiomeResolver biomeResolver,
        @NonNull TreeGrowthPolicyProvider policyProvider,
        @NonNull CurrentSeasonQuery currentSeasonQuery
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
    }

    public TreeGrowthDecision decideNaturalGrowth(@NonNull BlockPosition position) {
        TreeGrowthPolicy policy;
        try {
            policy = policyProvider.policyFor(biomeResolver.resolve(position).biomeId());
        } catch (UnsupportedBiomeException | UnsupportedTreeGrowthPolicyException exception) {
            return TreeGrowthDecision.ALLOW_GROWTH;
        }

        return policy.decide(currentSeasonQuery.currentSeason());
    }
}
