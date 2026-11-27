package io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnimalBreedingPolicyTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final DoubleSupplier UNEXPECTED_ROLL = () -> {
        throw new AssertionError("No roll is needed for a certain outcome");
    };

    @Test
    void allowsBreedingInAnUnconfiguredSeasonWithoutRolling() {
        AnimalBreedingPolicy policy = new AnimalBreedingPolicy(Map.of(WINTER, 0.5));

        assertTrue(policy.allowsBreeding(SPRING, UNEXPECTED_ROLL));
    }

    @Test
    void resolvesZeroAndFullChancesWithoutRolling() {
        AnimalBreedingPolicy policy = new AnimalBreedingPolicy(
            Map.of(WINTER, 0.0, SPRING, 1.0)
        );

        assertFalse(policy.allowsBreeding(WINTER, UNEXPECTED_ROLL));
        assertTrue(policy.allowsBreeding(SPRING, UNEXPECTED_ROLL));
    }

    @Test
    void allowsBreedingOnlyForARollStrictlyBelowTheConfiguredChance() {
        AnimalBreedingPolicy policy = new AnimalBreedingPolicy(Map.of(WINTER, 0.5));

        assertTrue(policy.allowsBreeding(WINTER, () -> Math.nextDown(0.5)));
        assertFalse(policy.allowsBreeding(WINTER, () -> 0.5));
        assertFalse(policy.allowsBreeding(WINTER, () -> 0.75));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidConfiguredChances(double chance) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AnimalBreedingPolicy(Map.of(WINTER, chance))
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidRolls(double roll) {
        AnimalBreedingPolicy policy = new AnimalBreedingPolicy(Map.of(WINTER, 0.5));

        assertThrows(
            IllegalArgumentException.class,
            () -> policy.allowsBreeding(WINTER, () -> roll)
        );
    }
}
