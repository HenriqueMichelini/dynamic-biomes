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
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final SeasonCalendar CALENDAR = new SeasonCalendar(List.of(
        SPRING,
        SUMMER,
        new SeasonId("minecraft:autumn"),
        WINTER
    ));

    @Test
    void advancesToNextSeasonAndUpdatesCache() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            SPRING
        );
        CachedCurrentSeasonQuery currentSeasonQuery = new CachedCurrentSeasonQuery(
            SPRING
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository,
            currentSeasonQuery
        );

        SeasonId nextSeason = service.advance();

        assertEquals(SUMMER, nextSeason);
        assertEquals(SUMMER, repository.savedSeason);
        assertEquals(SUMMER, currentSeasonQuery.currentSeason());
    }

    @Test
    void wrapsFromLastSeasonToFirstAndUpdatesCache() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            WINTER
        );
        CachedCurrentSeasonQuery currentSeasonQuery = new CachedCurrentSeasonQuery(
            WINTER
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository,
            currentSeasonQuery
        );

        SeasonId nextSeason = service.advance();

        assertEquals(SPRING, nextSeason);
        assertEquals(SPRING, repository.savedSeason);
        assertEquals(SPRING, currentSeasonQuery.currentSeason());
    }

    @Test
    void doesNotUpdateCacheWhenPersistenceFails() {
        FailingSeasonStateRepository repository = new FailingSeasonStateRepository(
            SPRING
        );
        CachedCurrentSeasonQuery currentSeasonQuery = new CachedCurrentSeasonQuery(
            SPRING
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository,
            currentSeasonQuery
        );

        assertThrows(
            IllegalStateException.class,
            service::advance
        );

        assertEquals(SPRING, currentSeasonQuery.currentSeason());
    }

    @Test
    void failsWhenCurrentSeasonIsNotInCalendar() {
        SeasonId monsoon = new SeasonId("minecraft:monsoon");
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            monsoon
        );
        CachedCurrentSeasonQuery currentSeasonQuery = new CachedCurrentSeasonQuery(
            monsoon
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository,
            currentSeasonQuery
        );

        assertThrows(IllegalArgumentException.class, service::advance);
        assertEquals(monsoon, currentSeasonQuery.currentSeason());
    }

    private static final class RecordingSeasonStateRepository
        implements SeasonStateRepository {
        private SeasonId currentSeason;
        private SeasonId savedSeason;

        RecordingSeasonStateRepository(SeasonId currentSeason) {
            this.currentSeason = currentSeason;
        }

        @Override
        public Optional<SeasonId> findCurrentSeason() {
            return Optional.ofNullable(currentSeason);
        }

        @Override
        public void saveCurrentSeason(SeasonId seasonId) {
            this.savedSeason = seasonId;
            this.currentSeason = seasonId;
        }
    }

    private static final class FailingSeasonStateRepository
        implements SeasonStateRepository {
        private final SeasonId currentSeason;

        FailingSeasonStateRepository(SeasonId currentSeason) {
            this.currentSeason = currentSeason;
        }

        @Override
        public Optional<SeasonId> findCurrentSeason() {
            return Optional.of(currentSeason);
        }

        @Override
        public void saveCurrentSeason(SeasonId seasonId) {
            throw new IllegalStateException("Persistence failed");
        }
    }
}
