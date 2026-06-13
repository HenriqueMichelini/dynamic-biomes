package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.HashSet;
import java.util.List;

public record SeasonCalendar(List<SeasonId> seasons) {
    public SeasonCalendar {
        if (seasons == null || seasons.isEmpty()) {
            throw new IllegalArgumentException("Season calendar requires at least one season");
        }
        if (seasons.stream().anyMatch(season -> season == null)) {
            throw new IllegalArgumentException("Season calendar cannot contain null seasons");
        }
        if (new HashSet<>(seasons).size() != seasons.size()) {
            throw new IllegalArgumentException("Season calendar cannot contain duplicate seasons");
        }

        seasons = List.copyOf(seasons);
    }

    public SeasonId nextAfter(SeasonId currentSeason) {
        int currentIndex = seasons.indexOf(currentSeason);
        if (currentIndex < 0) {
            throw new IllegalArgumentException("Unknown season: " + currentSeason);
        }

        return seasons.get((currentIndex + 1) % seasons.size());
    }
}
