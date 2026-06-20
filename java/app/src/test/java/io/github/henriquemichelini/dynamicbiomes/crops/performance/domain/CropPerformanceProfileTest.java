package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CropPerformanceProfileTest {

    @Test
    void storesCropOwnedEnvironmentalPreferences() {
        NormalizedEnvironmentalValue windSpeed = preference(0.1);
        NormalizedEnvironmentalValue rainStrength = preference(0.2);
        NormalizedEnvironmentalValue humidity = preference(0.3);
        NormalizedEnvironmentalValue temperature = preference(0.4);
        NormalizedEnvironmentalValue solarIncidence = preference(0.5);
        NormalizedEnvironmentalValue soilFertility = preference(0.6);

        CropPerformanceProfile profile = new CropPerformanceProfile(
            CropKind.WHEAT,
            windSpeed,
            rainStrength,
            humidity,
            temperature,
            solarIncidence,
            soilFertility
        );

        assertAll(
            () -> assertEquals(CropKind.WHEAT, profile.cropKind()),
            () -> assertEquals(windSpeed, profile.preferredWindSpeed()),
            () -> assertEquals(rainStrength, profile.preferredRainStrength()),
            () -> assertEquals(humidity, profile.preferredHumidity()),
            () -> assertEquals(temperature, profile.preferredTemperature()),
            () -> assertEquals(solarIncidence, profile.preferredSolarIncidence()),
            () -> assertEquals(soilFertility, profile.preferredSoilFertility())
        );
    }

    @Test
    void rejectsMissingPreferenceValues() {
        NormalizedEnvironmentalValue value = preference(0.5);

        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                null,
                value,
                value,
                value,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                null,
                value,
                value,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                value,
                null,
                value,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                value,
                value,
                null,
                value,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                value,
                value,
                value,
                null,
                value,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                value,
                value,
                value,
                value,
                null,
                value
            )),
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceProfile(
                CropKind.WHEAT,
                value,
                value,
                value,
                value,
                value,
                null
            ))
        );
    }

    @Test
    void rejectsInvalidPreferenceValuesThroughNormalizedValue() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> preference(-0.01)),
            () -> assertThrows(IllegalArgumentException.class, () -> preference(1.01)),
            () -> assertThrows(IllegalArgumentException.class, () -> preference(Double.NaN)),
            () -> assertThrows(IllegalArgumentException.class, () -> preference(Double.NEGATIVE_INFINITY)),
            () -> assertThrows(IllegalArgumentException.class, () -> preference(Double.POSITIVE_INFINITY))
        );
    }

    @Test
    void exposesOnlyCropAndNormalizedPreferenceComponents() {
        Set<Class<?>> componentTypes = Arrays.stream(CropPerformanceProfile.class.getRecordComponents())
            .map(RecordComponent::getType)
            .collect(Collectors.toSet());

        assertEquals(Set.of(CropKind.class, NormalizedEnvironmentalValue.class), componentTypes);
    }

    private static NormalizedEnvironmentalValue preference(double value) {
        return new NormalizedEnvironmentalValue(value);
    }
}
