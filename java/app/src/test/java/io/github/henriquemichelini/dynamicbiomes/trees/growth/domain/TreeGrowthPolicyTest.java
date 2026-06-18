package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TreeGrowthPolicyTest {
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void alwaysAllowsGrowthAtFullChanceWithoutVariation() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(1.0),
            () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }
        );

        TreeGrowthDecision decision = policy.decide();

        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void alwaysCancelsGrowthAtZeroChanceWithoutVariation() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.0),
            () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            }
        );

        TreeGrowthDecision decision = policy.decide();

        assertEquals(TreeGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void allowsGrowthWhenVariationIsLowerThanChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            () -> 0.49
        );

        TreeGrowthDecision decision = policy.decide();

        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsEqualToChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            () -> 0.5
        );

        TreeGrowthDecision decision = policy.decide();

        assertEquals(TreeGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void cancelsGrowthWhenVariationIsGreaterThanChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            () -> 0.51
        );

        TreeGrowthDecision decision = policy.decide();

        assertEquals(TreeGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void rejectsVariationOutsideUnitInterval() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthPolicy(
                    new TreeGrowthChance(0.5),
                    () -> -0.1
                ).decide()
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthPolicy(
                    new TreeGrowthChance(0.5),
                    () -> 1.1
                ).decide()
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthPolicy(
                    new TreeGrowthChance(0.5),
                    () -> Double.NaN
                ).decide()
            )
        );
    }

    @Test
    void appliesConfiguredSeasonalFactorToEffectiveChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            Map.of(SUMMER, new TreeGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        TreeGrowthChance effectiveChance = policy.effectiveChanceFor(SUMMER);

        assertEquals(0.75, effectiveChance.value());
    }

    @Test
    void defaultsSeasonalFactorToOneWhenSeasonIsNotConfigured() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            Map.of(SUMMER, new TreeGrowthSeasonalFactor(1.5)),
            () -> 0.0
        );

        TreeGrowthChance effectiveChance = policy.effectiveChanceFor(WINTER);

        assertEquals(0.5, effectiveChance.value());
        assertTrue(policy.seasonalFactorFor(WINTER).isEmpty());
    }

    @Test
    void capsEffectiveSeasonalChanceAtFullChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.75),
            Map.of(SUMMER, new TreeGrowthSeasonalFactor(2.0)),
            () -> 0.0
        );

        TreeGrowthChance effectiveChance = policy.effectiveChanceFor(SUMMER);

        assertEquals(1.0, effectiveChance.value());
    }

    @Test
    void decidesAgainstEffectiveSeasonalChance() {
        TreeGrowthPolicy policy = new TreeGrowthPolicy(
            new TreeGrowthChance(0.5),
            Map.of(SUMMER, new TreeGrowthSeasonalFactor(1.5)),
            () -> 0.74
        );

        TreeGrowthDecision decision = policy.decide(SUMMER);

        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void rejectsInvalidChanceAndSeasonalFactorValues() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthChance(-0.1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthChance(1.1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthChance(Double.NaN)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthSeasonalFactor(-0.1)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new TreeGrowthSeasonalFactor(Double.POSITIVE_INFINITY)
            )
        );
    }
}
