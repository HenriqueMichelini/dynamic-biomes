package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import org.junit.jupiter.api.Test;

class CurrentSeasonQueryTest {
    @Test
    void exposesCurrentSeasonAsPureDomainIdentity() {
        SeasonId currentSeason = new SeasonId("dynamicbiomes:spring");
        CurrentSeasonQuery query = () -> currentSeason;

        assertEquals(currentSeason, query.currentSeason());
    }
}
