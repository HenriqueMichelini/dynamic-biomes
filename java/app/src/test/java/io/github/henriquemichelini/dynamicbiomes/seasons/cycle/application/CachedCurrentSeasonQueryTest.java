package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import org.junit.jupiter.api.Test;

class CachedCurrentSeasonQueryTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");

    @Test
    void returnsInitialSeason() {
        CachedCurrentSeasonQuery query = new CachedCurrentSeasonQuery(SPRING);

        assertEquals(SPRING, query.currentSeason());
    }

    @Test
    void returnsUpdatedSeasonAfterUpdate() {
        CachedCurrentSeasonQuery query = new CachedCurrentSeasonQuery(SPRING);

        query.updateCurrentSeason(SUMMER);

        assertEquals(SUMMER, query.currentSeason());
    }

    @Test
    void rejectsNullInitialSeason() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CachedCurrentSeasonQuery(null)
        );
    }

    @Test
    void rejectsNullUpdatedSeason() {
        CachedCurrentSeasonQuery query = new CachedCurrentSeasonQuery(SPRING);

        assertThrows(
            IllegalArgumentException.class,
            () -> query.updateCurrentSeason(null)
        );
    }
}
