package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CropYieldDomainTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void validatesMultiplierRange() {
        assertThrows(IllegalArgumentException.class, () -> new CropYieldMultiplierRange(-0.1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new CropYieldMultiplierRange(1.1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new CropYieldMultiplierRange(1.0, Double.NaN));
    }

    @Test
    void selectsMultiplierWithinConfiguredRange() {
        CropYieldMultiplierCalculator calculator = new CropYieldMultiplierCalculator(() -> 0.25);

        assertEquals(1.5, calculator.calculate(new CropYieldMultiplierRange(1.0, 3.0)));
        assertEquals(2.0, calculator.calculate(new CropYieldMultiplierRange(2.0, 2.0)));
    }

    @Test
    void calculatesQuantityWithProbabilisticRounding() {
        assertEquals(
            3,
            new CropYieldQuantityCalculator(() -> 0.49).calculate(2, 1.25)
        );
        assertEquals(
            2,
            new CropYieldQuantityCalculator(() -> 0.50).calculate(2, 1.25)
        );
    }

    @Test
    void permitsZeroAdjustedProduceQuantity() {
        assertEquals(
            0,
            new CropYieldQuantityCalculator(() -> {
                throw new AssertionError("No variation needed for exact zero");
            }).calculate(3, 0.0)
        );
    }

    @Test
    void rejectsInvalidQuantitiesAndOverflow() {
        CropYieldQuantityCalculator calculator = new CropYieldQuantityCalculator(() -> 0.0);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(-1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(1, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(Integer.MAX_VALUE, 2.0));
    }

    @Test
    void appliesSeasonalFactorOrNeutralDefault() {
        CropYieldPolicy policy = new CropYieldPolicy(
            FOREST,
            Map.of(
                CropKind.WHEAT,
                new CropYieldCropRule(
                    new CropYieldMultiplierRange(1.0, 1.0),
                    Map.of(SPRING, new CropYieldSeasonalFactor(1.25))
                )
            )
        );

        assertEquals(1.25, policy.seasonalFactorFor(CropKind.WHEAT, SPRING));
        assertEquals(1.0, policy.seasonalFactorFor(CropKind.WHEAT, WINTER));
        assertThrows(
            UnsupportedCropYieldPolicyException.class,
            () -> policy.multiplierRangeFor(CropKind.CARROTS)
        );
    }

    @Test
    void rejectsInvalidSeasonalFactors() {
        assertThrows(IllegalArgumentException.class, () -> new CropYieldSeasonalFactor(0.0));
        assertThrows(IllegalArgumentException.class, () -> new CropYieldSeasonalFactor(Double.POSITIVE_INFINITY));
    }
}
