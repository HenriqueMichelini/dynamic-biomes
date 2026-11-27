package io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.function.DoubleSupplier;

public record AnimalBreedingPolicy(Map<SeasonId, Double> seasonalChances) {
    public AnimalBreedingPolicy {
        seasonalChances = Map.copyOf(seasonalChances);
        seasonalChances.forEach((season, chance) ->
            requireUnitValue(chance, "Breeding chance for " + season.value())
        );
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
        requireUnitValue(roll, "Breeding chance roll");
        return roll < chance;
    }

    private static void requireUnitValue(double value, String description) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(description + " must be within [0.0, 1.0]");
        }
    }
}
