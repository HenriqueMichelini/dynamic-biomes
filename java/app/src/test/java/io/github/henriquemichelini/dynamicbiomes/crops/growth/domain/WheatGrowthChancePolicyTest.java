package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WheatGrowthChancePolicyTest {
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

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

    @Test
    void returnsBaseChanceWhenSeasonHasNoConfiguredFactor() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.5),
            Map.of(SUMMER, new WheatGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        WheatGrowthChance chance = policy.effectiveChanceFor(WINTER);

        assertEquals(0.5, chance.value());
    }

    @Test
    void appliesZeroSeasonalFactor() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.5),
            Map.of(WINTER, new WheatGrowthSeasonalFactor(0.0)),
            () -> 0.0
        );

        WheatGrowthChance chance = policy.effectiveChanceFor(WINTER);

        assertEquals(0.0, chance.value());
    }

    @Test
    void appliesConfiguredSeasonalFactor() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.5),
            Map.of(SUMMER, new WheatGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        WheatGrowthChance chance = policy.effectiveChanceFor(SUMMER);

        assertEquals(0.75, chance.value());
    }

    @Test
    void capsEffectiveSeasonalChanceAtFullChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.75),
            Map.of(SUMMER, new WheatGrowthSeasonalFactor(2.0)),
            () -> 0.0
        );

        WheatGrowthChance chance = policy.effectiveChanceFor(SUMMER);

        assertEquals(1.0, chance.value());
    }

    @Test
    void decidesAgainstEffectiveSeasonalChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.5),
            Map.of(SUMMER, new WheatGrowthSeasonalFactor(1.5)),
            () -> 0.74
        );

        WheatGrowthDecision decision = policy.decide(SUMMER);

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void noSeasonDecisionKeepsUsingConfiguredBaseChance() {
        WheatGrowthChancePolicy policy = new WheatGrowthChancePolicy(
            new WheatGrowthChance(0.5),
            Map.of(SUMMER, new WheatGrowthSeasonalFactor(2.0)),
            () -> 0.75
        );

        WheatGrowthDecision decision = policy.decide();

        assertEquals(WheatGrowthDecision.CANCEL_GROWTH, decision);
    }
}
