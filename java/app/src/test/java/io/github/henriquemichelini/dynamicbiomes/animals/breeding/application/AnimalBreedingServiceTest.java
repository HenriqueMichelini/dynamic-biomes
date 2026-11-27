package io.github.henriquemichelini.dynamicbiomes.animals.breeding.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicy;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnimalBreedingServiceTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void usesCurrentSeasonAndPolicyForTheRequestedAnimal() {
        AtomicReference<SeasonId> season = new AtomicReference<>(SPRING);
        Map<AnimalKind, AnimalBreedingPolicy> policies = Map.of(
            AnimalKind.COW, new AnimalBreedingPolicy(Map.of(WINTER, 0.4)),
            AnimalKind.SHEEP, new AnimalBreedingPolicy(Map.of(WINTER, 0.8))
        );
        AnimalBreedingService service = new AnimalBreedingService(
            animal -> Optional.ofNullable(policies.get(animal)),
            season::get,
            () -> 0.5
        );

        assertTrue(service.allowsBreeding(AnimalKind.COW));
        season.set(WINTER);
        assertFalse(service.allowsBreeding(AnimalKind.COW));
        assertTrue(service.allowsBreeding(AnimalKind.SHEEP));
    }

    @Test
    void preservesVanillaForAnUnconfiguredAnimalWithoutQueryingSeasonOrRolling() {
        AnimalBreedingService service = new AnimalBreedingService(
            animal -> Optional.empty(),
            () -> { throw new AssertionError("No policy is configured"); },
            () -> { throw new AssertionError("No policy is configured"); }
        );

        assertTrue(service.allowsBreeding(AnimalKind.PIG));
    }
}
