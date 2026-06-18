package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnsupportedCropGrowthPolicyExceptionTest {
    @Test
    void preservesDiagnosticMessage() {
        UnsupportedCropGrowthPolicyException exception =
            new UnsupportedCropGrowthPolicyException(
                "No crop growth policy configured for biome minecraft:plains and crop wheat"
            );

        assertEquals(
            "No crop growth policy configured for biome minecraft:plains and crop wheat",
            exception.getMessage()
        );
    }
}
