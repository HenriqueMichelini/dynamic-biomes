package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import org.junit.jupiter.api.Test;

class SeasonProfileTest {
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final SeasonClimateAdjustment WINTER_CLIMATE = new SeasonClimateAdjustment(
        new SeasonalAdjustment(-0.7),
        new SeasonalAdjustment(0.1)
    );

    @Test
    void describesSeasonClimateAdjustment() {
        SeasonProfile profile = new SeasonProfile(WINTER, WINTER_CLIMATE);

        assertAll(
            () -> assertEquals(WINTER, profile.seasonId()),
            () -> assertEquals(WINTER_CLIMATE, profile.climateAdjustment())
        );
    }

    @Test
    void rejectsMissingSeasonIdentityOrClimateAdjustment() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new SeasonProfile(null, WINTER_CLIMATE)),
            () -> assertThrows(NullPointerException.class, () -> new SeasonProfile(WINTER, null))
        );
    }
}
