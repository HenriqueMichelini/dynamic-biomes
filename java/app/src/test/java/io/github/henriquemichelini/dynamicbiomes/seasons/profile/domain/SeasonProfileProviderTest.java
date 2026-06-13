package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import org.junit.jupiter.api.Test;

class SeasonProfileProviderTest {
    @Test
    void providesTypedProfileBySeasonIdentity() {
        SeasonId seasonId = new SeasonId("minecraft:spring");
        SeasonProfile expected = new SeasonProfile(
            seasonId,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(0.2),
                new SeasonalAdjustment(0.3)
            )
        );
        SeasonProfileProvider provider = requestedSeasonId -> {
            assertEquals(seasonId, requestedSeasonId);
            return expected;
        };

        assertEquals(expected, provider.profileFor(seasonId));
    }
}
