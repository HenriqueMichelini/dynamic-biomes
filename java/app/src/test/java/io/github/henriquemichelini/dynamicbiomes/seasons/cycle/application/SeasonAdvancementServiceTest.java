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
    private static final SeasonCalendar CALENDAR = new SeasonCalendar(List.of(
        new SeasonId("minecraft:spring"),
        new SeasonId("minecraft:summer"),
        new SeasonId("minecraft:autumn"),
        new SeasonId("minecraft:winter")
    ));

    @Test
    void advancesToNextSeason() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            new SeasonId("minecraft:spring")
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository
        );

        SeasonId nextSeason = service.advance();

        assertEquals(new SeasonId("minecraft:summer"), nextSeason);
        assertEquals(new SeasonId("minecraft:summer"), repository.savedSeason);
    }

    @Test
    void wrapsFromLastSeasonToFirst() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            new SeasonId("minecraft:winter")
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository
        );

        SeasonId nextSeason = service.advance();

        assertEquals(new SeasonId("minecraft:spring"), nextSeason);
        assertEquals(new SeasonId("minecraft:spring"), repository.savedSeason);
    }

    @Test
    void failsWhenCurrentSeasonIsNotInitialized() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(null);
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            service::advance
        );

        assertEquals("Current season is not initialized", exception.getMessage());
    }

    @Test
    void failsWhenCurrentSeasonIsNotInCalendar() {
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            new SeasonId("minecraft:monsoon")
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            CALENDAR,
            repository
        );

        assertThrows(IllegalArgumentException.class, service::advance);
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
}
