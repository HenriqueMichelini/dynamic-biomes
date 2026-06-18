package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CropGrowthDecisionTest {
    @Test
    void allowGrowthRepresentsNonCancelledGrowth() {
        assertEquals("ALLOW_GROWTH", CropGrowthDecision.ALLOW_GROWTH.name());
    }

    @Test
    void cancelGrowthRepresentsCancelledGrowth() {
        assertEquals("CANCEL_GROWTH", CropGrowthDecision.CANCEL_GROWTH.name());
    }

    @Test
    void allowAndCancelAreDistinctDecisions() {
        assertNotEquals(
            CropGrowthDecision.ALLOW_GROWTH,
            CropGrowthDecision.CANCEL_GROWTH
        );
    }
}
