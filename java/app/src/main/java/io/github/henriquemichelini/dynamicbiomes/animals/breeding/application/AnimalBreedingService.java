package io.github.henriquemichelini.dynamicbiomes.animals.breeding.application;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicy;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class AnimalBreedingService {
    private final AnimalBreedingPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final DoubleSupplier variationSource;

    public AnimalBreedingService(
        AnimalBreedingPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        DoubleSupplier variationSource
    ) {
        this.policyProvider = Objects.requireNonNull(policyProvider);
        this.currentSeasonQuery = Objects.requireNonNull(currentSeasonQuery);
        this.variationSource = Objects.requireNonNull(variationSource);
    }

    public boolean allowsBreeding(AnimalKind animal) {
        AnimalBreedingPolicy policy = policyProvider.policyFor(animal).orElse(null);
        return policy == null || policy.allowsBreeding(currentSeasonQuery.currentSeason(), variationSource);
    }
}
