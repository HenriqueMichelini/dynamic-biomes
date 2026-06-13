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
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");

    @Test
    void runningTaskDelegatesToSeasonAdvancementService() {
        InMemorySeasonStateRepository repository = new InMemorySeasonStateRepository(SPRING);
        SeasonAdvancementService service = new SeasonAdvancementService(
            new SeasonCalendar(List.of(SPRING, SUMMER)),
            repository
        );
        Runnable task = new SeasonAdvancementTask(service);

        task.run();

        assertEquals(SUMMER, repository.findCurrentSeason().orElseThrow());
    }

    private static final class InMemorySeasonStateRepository implements SeasonStateRepository {
        private SeasonId currentSeason;

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
