package io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SeasonClimateAdjustmentTest {
    @Test
    void groupsTemperatureAndHumidityAdjustments() {
        SeasonalAdjustment temperature = new SeasonalAdjustment(-0.4);
        SeasonalAdjustment humidity = new SeasonalAdjustment(0.2);

        SeasonClimateAdjustment adjustment = new SeasonClimateAdjustment(temperature, humidity);

        assertAll(
            () -> assertEquals(temperature, adjustment.temperature()),
            () -> assertEquals(humidity, adjustment.humidity())
        );
    }

    @Test
    void rejectsMissingClimateAdjustments() {
        SeasonalAdjustment neutral = new SeasonalAdjustment(0.0);

        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new SeasonClimateAdjustment(null, neutral)),
            () -> assertThrows(NullPointerException.class, () -> new SeasonClimateAdjustment(neutral, null))
        );
    }
}
