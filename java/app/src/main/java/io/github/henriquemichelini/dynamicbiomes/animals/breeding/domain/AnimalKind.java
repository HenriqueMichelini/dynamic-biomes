package io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain;

import java.util.Optional;

public enum AnimalKind {
    COW("cow"),
    SHEEP("sheep"),
    PIG("pig"),
    CHICKEN("chicken");

    private final String policyKey;

    AnimalKind(String policyKey) {
        this.policyKey = policyKey;
    }

    public static Optional<AnimalKind> fromPolicyKey(String policyKey) {
        for (AnimalKind animal : values()) {
            if (animal.policyKey.equals(policyKey)) {
                return Optional.of(animal);
            }
        }
        return Optional.empty();
    }
}
