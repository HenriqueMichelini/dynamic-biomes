package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CropEnvironmentalStateTest {

    @Test
    void storesCropOwnedEnvironmentalValues() {
        NormalizedEnvironmentalValue windSpeed = new NormalizedEnvironmentalValue(0.1);
        NormalizedEnvironmentalValue rainStrength = new NormalizedEnvironmentalValue(0.2);
        NormalizedEnvironmentalValue humidity = new NormalizedEnvironmentalValue(0.3);
        NormalizedEnvironmentalValue temperature = new NormalizedEnvironmentalValue(0.4);
        NormalizedEnvironmentalValue solarIncidence = new NormalizedEnvironmentalValue(0.5);
        NormalizedEnvironmentalValue soilFertility = new NormalizedEnvironmentalValue(0.6);

        CropEnvironmentalState state = new CropEnvironmentalState(
            windSpeed,
            rainStrength,
            humidity,
            temperature,
            solarIncidence,
            soilFertility
        );

        assertAll(
            () -> assertEquals(windSpeed, state.windSpeed()),
            () -> assertEquals(rainStrength, state.rainStrength()),
            () -> assertEquals(humidity, state.humidity()),
            () -> assertEquals(temperature, state.temperature()),
            () -> assertEquals(solarIncidence, state.solarIncidence()),
            () -> assertEquals(soilFertility, state.soilFertility())
        );
    }

    @Test
    void retainsInclusiveNormalizedBounds() {
        assertAll(
            () -> assertEquals(0.0, new NormalizedEnvironmentalValue(0.0).normalized()),
            () -> assertEquals(1.0, new NormalizedEnvironmentalValue(1.0).normalized())
        );
    }

    @Test
    void rejectsInvalidNormalizedValues() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new NormalizedEnvironmentalValue(-0.01)),
            () -> assertThrows(IllegalArgumentException.class, () -> new NormalizedEnvironmentalValue(1.01)),
            () -> assertThrows(IllegalArgumentException.class, () -> new NormalizedEnvironmentalValue(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> new NormalizedEnvironmentalValue(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> new NormalizedEnvironmentalValue(Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void rejectsMissingEnvironmentalValues() {
        NormalizedEnvironmentalValue value = new NormalizedEnvironmentalValue(0.5);

        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                null,
                value,
                value,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                value,
                null,
                value,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                value,
                value,
                null,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                value,
                value,
                value,
                null,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                value,
                value,
                value,
                value,
                null,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropEnvironmentalState(
                value,
                value,
                value,
                value,
                value,
                null
            ))
        );
    }
}
