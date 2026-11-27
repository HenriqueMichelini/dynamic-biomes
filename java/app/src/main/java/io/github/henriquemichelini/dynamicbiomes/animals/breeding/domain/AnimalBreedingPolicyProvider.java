package io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain;

import java.util.Optional;

public interface AnimalBreedingPolicyProvider {
    Optional<AnimalBreedingPolicy> policyFor(AnimalKind animal);
}
