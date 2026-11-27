package io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState.DayPhase;
import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState.SkyWeather;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.EcologicalRegionId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EcologicalRegionStateTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(new UUID(0, 1)), -3, 64, 128
    );

    @ParameterizedTest
    @CsvSource({
        "0, DAY",
        "11999, DAY",
        "12000, SUNSET",
        "12999, SUNSET",
        "13000, NIGHT",
        "22999, NIGHT",
        "23000, SUNRISE",
        "23999, SUNRISE"
    })
    void reportsObservedWeatherAndDayPhaseForThePositionRegion(
        long timeOfDay,
        DayPhase dayPhase
    ) {
        assertEquals(
            new EcologicalRegionState(
                new EcologicalRegionId(POSITION.world(), -1, 1),
                SkyWeather.STORM,
                dayPhase
            ),
            EcologicalRegionState.observedAt(POSITION, SkyWeather.STORM, timeOfDay)
        );
    }

    @Test
    void rejectsTicksOutsideOneMinecraftDay() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EcologicalRegionState.observedAt(POSITION, SkyWeather.CLEAR, -1)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> EcologicalRegionState.observedAt(POSITION, SkyWeather.CLEAR, 24000)
        );
    }
}
