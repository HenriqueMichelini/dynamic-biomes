package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.HashSet;
import java.util.List;

public record SeasonCalendar(List<SeasonId> seasons) {
    private static final SeasonCalendar STANDARD = new SeasonCalendar(List.of(
        new SeasonId("minecraft:spring"),
        new SeasonId("minecraft:summer"),
        new SeasonId("minecraft:autumn"),
        new SeasonId("minecraft:winter")
    ));

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

    public static SeasonCalendar standard() {
        return STANDARD;
    }

    public SeasonId nextAfter(SeasonId currentSeason) {
        int currentIndex = currentSeason == null ? -1 : seasons.indexOf(currentSeason);
        if (currentIndex < 0) {
            throw new IllegalArgumentException("Unknown season: " + currentSeason);
        }

        return seasons.get((currentIndex + 1) % seasons.size());
    }

    public boolean contains(SeasonId season) {
        return season != null && seasons.contains(season);
    }
}
