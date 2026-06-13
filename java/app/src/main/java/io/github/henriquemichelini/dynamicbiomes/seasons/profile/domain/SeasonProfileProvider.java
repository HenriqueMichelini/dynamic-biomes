package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public interface SeasonProfileProvider {
    SeasonProfile profileFor(SeasonId seasonId);
}
