package io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.EcologicalRegionId;
import lombok.NonNull;

/** World-wide sky conditions observed for a resolved ecological region. */
public record EcologicalRegionState(
    @NonNull EcologicalRegionId regionId,
    @NonNull SkyWeather weather,
    @NonNull DayPhase dayPhase
) {
    public EcologicalRegionState {}

    public static EcologicalRegionState observedAt(
        BlockPosition position,
        SkyWeather weather,
        long timeOfDay
    ) {
        return new EcologicalRegionState(
            EcologicalRegionId.containing(position),
            weather,
            DayPhase.at(timeOfDay)
        );
    }

    public enum SkyWeather {
        CLEAR,
        STORM,
        THUNDER
    }

    /** Phase of the world clock, independent of local sky exposure. */
    public enum DayPhase {
        DAY,
        SUNSET,
        NIGHT,
        SUNRISE;

        private static DayPhase at(long timeOfDay) {
            if (timeOfDay < 0 || timeOfDay >= 24000) {
                throw new IllegalArgumentException(
                    "Time of day must be between 0 and 23999 ticks"
                );
            }
            if (timeOfDay < 12000) {
                return DAY;
            }
            if (timeOfDay < 13000) {
                return SUNSET;
            }
            if (timeOfDay < 23000) {
                return NIGHT;
            }
            return SUNRISE;
        }
    }
}
