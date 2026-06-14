package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonAdvancementService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeasonAdvancementTaskTest {

    @Test
    void runningTaskDelegatesToSeasonAdvancementService() {
        SeasonCalendar calendar = new SeasonCalendar(List.of(
            new SeasonId("minecraft:spring"),
            new SeasonId("minecraft:summer")
        ));
        RecordingSeasonStateRepository repository = new RecordingSeasonStateRepository(
            new SeasonId("minecraft:spring")
        );
        SeasonAdvancementService service = new SeasonAdvancementService(
            calendar,
            repository
        );
        Runnable task = new SeasonAdvancementTask(service);

        task.run();

        assertEquals(new SeasonId("minecraft:summer"), repository.savedSeason);
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
