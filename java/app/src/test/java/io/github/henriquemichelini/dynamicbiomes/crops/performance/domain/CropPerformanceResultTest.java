package io.github.henriquemichelini.dynamicbiomes.crops.performance.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CropPerformanceResultTest {

    @Test
    void exposesOnlyScoreAndBehaviorFactors() {
        Set<String> componentNames = Arrays.stream(CropPerformanceResult.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());

        assertEquals(
            Set.of("overallScore", "growthSpeedFactor", "growthChanceFactor", "harvestQuantityFactor"),
            componentNames
        );
    }

    @Test
    void exposesOptionalOverallScore() {
        CropPerformanceResult result = new CropPerformanceResult(OptionalDouble.of(0.75), 0.8, 0.7, 0.6);

        assertAll(
            () -> assertEquals(0.75, result.overallScore().orElseThrow()),
            () -> assertEquals(0.8, result.growthSpeedFactor()),
            () -> assertEquals(0.7, result.growthChanceFactor()),
            () -> assertEquals(0.6, result.harvestQuantityFactor())
        );
    }

    @Test
    void rejectsInvalidPerformanceNumbers() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new CropPerformanceResult(null, 1.0, 1.0, 1.0)),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(-0.01),
                1.0,
                1.0,
                1.0
            )),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(1.01),
                1.0,
                1.0,
                1.0
            )),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(Double.NaN),
                1.0,
                1.0,
                1.0
            )),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(1.0),
                -0.01,
                1.0,
                1.0
            )),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(1.0),
                1.0,
                Double.POSITIVE_INFINITY,
                1.0
            )),
            () -> assertThrows(IllegalArgumentException.class, () -> new CropPerformanceResult(
                OptionalDouble.of(1.0),
                1.0,
                1.0,
                Double.NaN
            ))
        );
    }

    @Test
    void publicResultDoesNotExposeQualityConcepts() {
        Set<String> componentNames = Arrays.stream(CropPerformanceResult.class.getRecordComponents())
            .map(RecordComponent::getName)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        assertFalse(componentNames.stream().anyMatch(name -> name.contains("quality")));
    }
}
