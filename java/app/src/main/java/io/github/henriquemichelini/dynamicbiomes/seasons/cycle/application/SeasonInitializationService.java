package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Optional;

public final class SeasonInitializationService {
    private final SeasonCalendar calendar;
    private final SeasonStateRepository repository;

    public SeasonInitializationService(SeasonCalendar calendar, SeasonStateRepository repository) {
        this.calendar = calendar;
        this.repository = repository;
    }

    public SeasonId initializeIfMissing() {
        Optional<SeasonId> currentSeason = repository.findCurrentSeason();
        if (currentSeason.isPresent()) {
            return currentSeason.get();
        }

        SeasonId firstSeason = calendar.seasons().get(0);
        repository.saveCurrentSeason(firstSeason);
        return firstSeason;
    }
}
