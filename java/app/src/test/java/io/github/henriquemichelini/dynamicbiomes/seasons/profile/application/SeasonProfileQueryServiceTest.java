package io.github.henriquemichelini.dynamicbiomes.seasons.profile.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import org.junit.jupiter.api.Test;

class SeasonProfileQueryServiceTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void returnsMatchingSeasonProfileWhenCurrentSeasonExists() {
        SeasonProfile expected = new SeasonProfile(
            SPRING,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(0.2),
                new SeasonalAdjustment(0.3)
            )
        );
        CurrentSeasonQuery currentSeasonQuery = () -> SPRING;
        SeasonProfileProvider provider = seasonId -> {
            if (seasonId.equals(SPRING)) {
                return expected;
            }
            throw new IllegalArgumentException("Missing season profile: " + seasonId.value());
        };
        SeasonProfileQueryService service = new SeasonProfileQueryService(
            currentSeasonQuery,
            provider
        );

        SeasonProfile result = service.currentProfile();

        assertEquals(expected, result);
    }

    @Test
    void propagatesProviderExceptionWhenProfileIsMissing() {
        CurrentSeasonQuery currentSeasonQuery = () -> WINTER;
        SeasonProfileProvider provider = seasonId -> {
            throw new IllegalArgumentException("Missing season profile: " + seasonId.value());
        };
        SeasonProfileQueryService service = new SeasonProfileQueryService(
            currentSeasonQuery,
            provider
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            service::currentProfile
        );

        assertEquals("Missing season profile: " + WINTER.value(), exception.getMessage());
    }

    @Test
    void propagatesQueryExceptionWhenCurrentSeasonIsUninitialized() {
        CurrentSeasonQuery currentSeasonQuery = () -> {
            throw new IllegalStateException("Current season is not initialized");
        };
        SeasonProfileProvider provider = seasonId -> {
            throw new AssertionError("Provider should not be called when season is uninitialized");
        };
        SeasonProfileQueryService service = new SeasonProfileQueryService(
            currentSeasonQuery,
            provider
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            service::currentProfile
        );

        assertEquals("Current season is not initialized", exception.getMessage());
    }
}
