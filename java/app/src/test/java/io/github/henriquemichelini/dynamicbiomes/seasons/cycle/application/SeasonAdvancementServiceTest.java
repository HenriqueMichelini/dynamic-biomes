package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeasonAdvancementServiceTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId AUTUMN = new SeasonId("minecraft:autumn");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final SeasonCalendar CALENDAR = new SeasonCalendar(
        List.of(SPRING, SUMMER, AUTUMN, WINTER)
    );

    @Test
    void advancesAndPersistsCurrentSeason() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(SPRING);
        SeasonAdvancementService service = new SeasonAdvancementService(CALENDAR, repository);

        SeasonId advancedSeason = service.advance();

        assertEquals(SUMMER, advancedSeason);
        assertEquals(SUMMER, repository.findCurrentSeason().orElseThrow());
    }

    @Test
    void wrapsLastSeasonToFirstThroughCalendar() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(WINTER);
        SeasonAdvancementService service = new SeasonAdvancementService(CALENDAR, repository);

        assertEquals(SPRING, service.advance());
        assertEquals(SPRING, repository.findCurrentSeason().orElseThrow());
    }

    @Test
    void unknownCurrentSeasonFailsThroughCalendarWithoutSaving() {
        SeasonId unknown = new SeasonId("minecraft:monsoon");
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(unknown);
        SeasonAdvancementService service = new SeasonAdvancementService(CALENDAR, repository);

        assertThrows(IllegalArgumentException.class, service::advance);
        assertEquals(unknown, repository.findCurrentSeason().orElseThrow());
    }

    @Test
    void uninitializedCurrentSeasonFailsClearlyWithoutSaving() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository();
        SeasonAdvancementService service = new SeasonAdvancementService(CALENDAR, repository);

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::advance);

        assertEquals("Current season is not initialized", exception.getMessage());
        assertEquals(Optional.empty(), repository.findCurrentSeason());
    }

    private static final class InMemorySeasonStateRepository implements SeasonStateRepository {
        private SeasonId currentSeason;

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
        }
    }
}
