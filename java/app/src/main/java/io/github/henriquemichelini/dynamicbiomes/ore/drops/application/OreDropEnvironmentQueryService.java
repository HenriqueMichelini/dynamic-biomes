package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class OreDropEnvironmentQueryService {
    private final BiomeResolver biomeResolver;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final SeasonProfileProvider seasonProfileProvider;

    public OreDropEnvironmentQueryService(
        BiomeResolver biomeResolver,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider
    ) {
        this.biomeResolver = biomeResolver;
        this.currentSeasonQuery = currentSeasonQuery;
        this.seasonProfileProvider = seasonProfileProvider;
    }

    public OreDropEnvironmentContext resolve(BlockPosition position) {
        BiomeContext biomeContext = biomeResolver.resolve(position);
        SeasonId currentSeason = currentSeasonQuery.currentSeason();
        SeasonProfile seasonProfile = seasonProfileProvider.profileFor(currentSeason);
        return new OreDropEnvironmentContext(biomeContext, seasonProfile);
    }
}
