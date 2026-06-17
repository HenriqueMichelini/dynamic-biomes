package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CropGrowthPolicyTest {
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void alwaysAllowsGrowthAtFullChanceWithoutVariation() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }
        );

        CropGrowthDecision decision = policy.decide(new CropGrowthChance(1.0));

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void alwaysCancelsGrowthAtZeroChanceWithoutVariation() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            }
        );

        CropGrowthDecision decision = policy.decide(new CropGrowthChance(0.0));

        assertEquals(CropGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void allowsGrowthWhenVariationIsLowerThanChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(() -> 0.49);

        CropGrowthDecision decision = policy.decide(new CropGrowthChance(0.5));

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsEqualToChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(() -> 0.5);

        CropGrowthDecision decision = policy.decide(new CropGrowthChance(0.5));

        assertEquals(CropGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsGreaterThanChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(() -> 0.51);

        CropGrowthDecision decision = policy.decide(new CropGrowthChance(0.5));

        assertEquals(CropGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void rejectsVariationOutsideUnitInterval() {
        CropGrowthChance chance = new CropGrowthChance(0.5);

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthPolicy(() -> -0.1).decide(chance)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthPolicy(() -> 1.1).decide(chance)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CropGrowthPolicy(() -> Double.NaN).decide(chance)
            )
        );
    }

    @Test
    void returnsBaseChanceWhenSeasonHasNoConfiguredFactor() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        CropGrowthChance chance = policy.effectiveChanceFor(WINTER);

        assertEquals(0.5, chance.value());
    }

    @Test
    void returnsConfiguredSeasonalFactorForSeason() {
        CropGrowthSeasonalFactor factor = new CropGrowthSeasonalFactor(1.5);
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, factor),
            () -> 0.0
        );

        assertEquals(factor, policy.seasonalFactorFor(SUMMER).orElseThrow());
    }

    @Test
    void returnsEmptySeasonalFactorWhenSeasonIsNotConfigured() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        assertTrue(policy.seasonalFactorFor(WINTER).isEmpty());
    }

    @Test
    void appliesZeroSeasonalFactor() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(WINTER, new CropGrowthSeasonalFactor(0.0)),
            () -> 0.0
        );

        CropGrowthChance chance = policy.effectiveChanceFor(WINTER);

        assertEquals(0.0, chance.value());
    }

    @Test
    void appliesConfiguredSeasonalFactor() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        CropGrowthChance chance = policy.effectiveChanceFor(SUMMER);

        assertEquals(0.75, chance.value());
    }

    @Test
    void capsEffectiveSeasonalChanceAtFullChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.75),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(2.0)),
            () -> 0.0
        );

        CropGrowthChance chance = policy.effectiveChanceFor(SUMMER);

        assertEquals(1.0, chance.value());
    }

    @Test
    void preservesSeasonalEffectiveChanceBehaviorThroughCropVocabulary() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.75),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(2.0)),
            () -> 0.0
        );

        assertEquals(1.0, policy.effectiveChanceFor(SUMMER).value());
    }

    @Test
    void decidesAgainstEffectiveSeasonalChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(1.5)),
            () -> 0.74
        );

        CropGrowthDecision decision = policy.decide(SUMMER);

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void noSeasonDecisionKeepsUsingConfiguredBaseChance() {
        CropGrowthPolicy policy = new CropGrowthPolicy(
            new CropGrowthChance(0.5),
            Map.of(SUMMER, new CropGrowthSeasonalFactor(2.0)),
            () -> 0.75
        );

        CropGrowthDecision decision = policy.decide();

        assertEquals(CropGrowthDecision.CANCEL_GROWTH, decision);
    }
}
