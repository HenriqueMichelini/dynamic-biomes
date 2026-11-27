package io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.function.DoubleSupplier;

public record AnimalBreedingPolicy(Map<SeasonId, Double> seasonalChances) {
    public AnimalBreedingPolicy {
        seasonalChances = Map.copyOf(seasonalChances);
        seasonalChances.forEach((season, chance) -> {
            if (!isUnitValue(chance)) {
                throw new InvalidAnimalBreedingPolicyException(
                    "Breeding chance for " + season.value() + " must be within [0.0, 1.0]"
                );
            }
        });
    }

    public boolean allowsBreeding(SeasonId season, DoubleSupplier variationSource) {
        double chance = seasonalChances.getOrDefault(season, 1.0);
        if (chance == 1.0) {
            return true;
        }
        if (chance == 0.0) {
            return false;
        }

        double roll = variationSource.getAsDouble();
        if (!isUnitValue(roll)) {
            throw new IllegalArgumentException("Breeding chance roll must be within [0.0, 1.0]");
        }
        return roll < chance;
    }

    private static boolean isUnitValue(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }
}
