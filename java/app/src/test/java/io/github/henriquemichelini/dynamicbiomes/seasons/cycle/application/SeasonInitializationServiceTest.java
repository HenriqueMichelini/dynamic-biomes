package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeasonInitializationServiceTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId AUTUMN = new SeasonId("minecraft:autumn");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final SeasonCalendar CALENDAR = new SeasonCalendar(
        List.of(SPRING, SUMMER, AUTUMN, WINTER)
    );

    @Test
    void returnsExistingSeasonWithoutSaving() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(SUMMER);
        SeasonInitializationService service = new SeasonInitializationService(CALENDAR, repository);

        SeasonId result = service.initializeIfMissing();

        assertEquals(SUMMER, result);
        assertEquals(SUMMER, repository.findCurrentSeason().orElseThrow());
        assertEquals(0, repository.saveCount);
    }

    @Test
    void savesAndReturnsFirstCalendarSeasonWhenMissing() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository();
        SeasonInitializationService service = new SeasonInitializationService(CALENDAR, repository);

        SeasonId result = service.initializeIfMissing();

        assertEquals(SPRING, result);
        assertEquals(SPRING, repository.findCurrentSeason().orElseThrow());
        assertEquals(1, repository.saveCount);
    }

    @Test
    void idempotentWhenCalledTwiceOnEmptyRepository() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository();
        SeasonInitializationService service = new SeasonInitializationService(CALENDAR, repository);

        SeasonId first = service.initializeIfMissing();
        SeasonId second = service.initializeIfMissing();

        assertEquals(SPRING, first);
        assertEquals(SPRING, second);
        assertEquals(1, repository.saveCount);
    }

    @Test
    void throwsWhenPersistedSeasonIsNotInCalendar() {
        SeasonId unknown = new SeasonId("minecraft:monsoon");
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(unknown);
        SeasonInitializationService service = new SeasonInitializationService(CALENDAR, repository);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            service::initializeIfMissing
        );

        assertEquals(
            "Persisted current season is not in configured calendar: minecraft:monsoon",
            exception.getMessage()
        );
        assertEquals(0, repository.saveCount);
    }

    @Test
    void rejectsEmptyCalendarThroughDomainValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SeasonCalendar(List.of())
        );
    }

    private static final class InMemorySeasonStateRepository implements SeasonStateRepository {
        private SeasonId currentSeason;
        private int saveCount;

        private InMemorySeasonStateRepository() {
        }

        private InMemorySeasonStateRepository(SeasonId currentSeason) {
            this.currentSeason = currentSeason;
        }

        @Override
        public Optional<SeasonId> findCurrentSeason() {
            return Optional.ofNullable(currentSeason);
        }

        @Override
        public void saveCurrentSeason(SeasonId seasonId) {
            currentSeason = seasonId;
            saveCount++;
        }
    }
}
