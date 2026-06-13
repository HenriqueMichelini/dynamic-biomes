package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeasonCalendarTest {
    private static final SeasonId SPRING = new SeasonId("dynamicbiomes:spring");
    private static final SeasonId SUMMER = new SeasonId("dynamicbiomes:summer");
    private static final SeasonId AUTUMN = new SeasonId("dynamicbiomes:autumn");
    private static final SeasonId WINTER = new SeasonId("dynamicbiomes:winter");

    @Test
    void requiresAtLeastOneSeason() {
        assertThrows(IllegalArgumentException.class, () -> new SeasonCalendar(List.of()));
    }

    @Test
    void rejectsDuplicateSeasonIds() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SeasonCalendar(List.of(SPRING, SUMMER, SPRING))
        );
    }

    @Test
    void returnsNextSeasonAfterKnownSeason() {
        SeasonCalendar calendar = standardCalendar();

        assertEquals(SUMMER, calendar.nextAfter(SPRING));
        assertEquals(AUTUMN, calendar.nextAfter(SUMMER));
    }

    @Test
    void rejectsUnknownCurrentSeason() {
        SeasonCalendar calendar = standardCalendar();
        SeasonId unknown = new SeasonId("dynamicbiomes:monsoon");

        assertThrows(IllegalArgumentException.class, () -> calendar.nextAfter(unknown));
    }

    @Test
    void wrapsFromLastSeasonBackToFirst() {
        SeasonCalendar calendar = standardCalendar();

        assertEquals(SPRING, calendar.nextAfter(WINTER));
    }

    @Test
    void exposesImmutableOrderedCycle() {
        SeasonCalendar calendar = standardCalendar();

        assertEquals(List.of(SPRING, SUMMER, AUTUMN, WINTER), calendar.seasons());
        assertThrows(UnsupportedOperationException.class, () -> calendar.seasons().add(SPRING));
    }

    private static SeasonCalendar standardCalendar() {
        return new SeasonCalendar(List.of(SPRING, SUMMER, AUTUMN, WINTER));
    }
}
