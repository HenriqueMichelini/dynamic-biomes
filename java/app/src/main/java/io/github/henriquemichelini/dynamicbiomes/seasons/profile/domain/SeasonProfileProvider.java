package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;

public interface SeasonProfileProvider {
    /** @throws MissingSeasonProfileException if the season has no profile */
    SeasonProfile profileFor(SeasonId seasonId);
}
