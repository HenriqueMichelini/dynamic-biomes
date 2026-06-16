package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class WheatGrowthChancePolicyTest {
    @Test
    void alwaysAllowsGrowthAtFullChanceWithoutVariation() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }
        );

        WheatGrowthDecision decision = policy.decide(new WheatGrowthChance(1.0));

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void alwaysCancelsGrowthAtZeroChanceWithoutVariation() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            }
        );

        WheatGrowthDecision decision = policy.decide(new WheatGrowthChance(0.0));

        assertEquals(WheatGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void allowsGrowthWhenVariationIsLowerThanChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(() -> 0.49);

        WheatGrowthDecision decision = policy.decide(new WheatGrowthChance(0.5));

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsEqualToChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(() -> 0.5);

        WheatGrowthDecision decision = policy.decide(new WheatGrowthChance(0.5));

        assertEquals(WheatGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsGreaterThanChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(() -> 0.51);

        WheatGrowthDecision decision = policy.decide(new WheatGrowthChance(0.5));

        assertEquals(WheatGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void rejectsVariationOutsideUnitInterval() {
        WheatGrowthChance chance = new WheatGrowthChance(0.5);

        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChancePolicy(() -> -0.1).decide(chance)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChancePolicy(() -> 1.1).decide(chance)),
            () -> assertThrows(IllegalArgumentException.class, () -> new WheatGrowthChancePolicy(() -> Double.NaN).decide(chance))
        );
    }
}
