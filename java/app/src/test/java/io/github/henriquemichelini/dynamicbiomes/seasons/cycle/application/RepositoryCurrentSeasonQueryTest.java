package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RepositoryCurrentSeasonQueryTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");

    @Test
    void returnsExistingCurrentSeason() {
        SeasonStateRepository repository = new InMemorySeasonStateRepository(SPRING);
        RepositoryCurrentSeasonQuery query = new RepositoryCurrentSeasonQuery(repository);

        SeasonId result = query.currentSeason();

        assertEquals(SPRING, result);
    }

    @Test
    void failsWhenCurrentSeasonIsMissing() {
        SeasonStateRepository repository = new InMemorySeasonStateRepository();
        RepositoryCurrentSeasonQuery query = new RepositoryCurrentSeasonQuery(repository);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            query::currentSeason
        );

        assertEquals("Current season is not initialized", exception.getMessage());
    }

    @Test
    void propagatesRepositoryFailure() {
        SeasonStateRepository repository = new SeasonStateRepository() {
            @Override
            public Optional<SeasonId> findCurrentSeason() {
                throw new IllegalStateException("Repository is unreachable");
            }

            @Override
            public void saveCurrentSeason(SeasonId seasonId) {
                throw new UnsupportedOperationException();
            }
        };
        RepositoryCurrentSeasonQuery query = new RepositoryCurrentSeasonQuery(repository);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            query::currentSeason
        );

        assertEquals("Repository is unreachable", exception.getMessage());
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
