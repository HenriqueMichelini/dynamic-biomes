package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClimateProfileTest {
    @Test
    void groupsHumidityAndTemperature() {
        Humidity humidity = new Humidity(0.7);
        Temperature temperature = new Temperature(0.4);

        ClimateProfile profile = new ClimateProfile(humidity, temperature);

        assertAll(
            () -> assertEquals(humidity, profile.humidity()),
            () -> assertEquals(temperature, profile.temperature())
        );
    }

    @Test
    void rejectsMissingClimateValues() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new ClimateProfile(null, new Temperature(0.5))),
            () -> assertThrows(NullPointerException.class, () -> new ClimateProfile(new Humidity(0.5), null))
        );
    }
}
