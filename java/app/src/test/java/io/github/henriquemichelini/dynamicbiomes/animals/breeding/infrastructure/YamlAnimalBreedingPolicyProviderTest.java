package io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlAnimalBreedingPolicyProviderTest {
    @TempDir
    Path tempDirectory;

    @Test
    void loadsExplicitAnimalAndSeasonPolicies() throws IOException {
        YamlAnimalBreedingPolicyProvider provider = load("""
            cow:
              minecraft:winter: 0.4
              server:dry-season: 0
            chicken:
              minecraft:winter: 0.8
            """);

        SeasonId winter = new SeasonId("minecraft:winter");
        var cowPolicy = provider.policyFor(AnimalKind.COW).orElseThrow();

        assertFalse(cowPolicy.allowsBreeding(winter, () -> 0.5));
        assertFalse(cowPolicy.allowsBreeding(new SeasonId("server:dry-season"), () -> 0.0));
        assertTrue(cowPolicy.allowsBreeding(new SeasonId("minecraft:spring"), () -> 0.5));
        assertTrue(provider.policyFor(AnimalKind.CHICKEN).orElseThrow().allowsBreeding(winter, () -> 0.5));
        assertTrue(provider.policyFor(AnimalKind.SHEEP).isEmpty());
    }

    @Test
    void loadsAnEmptyPolicyAsNeutral() throws IOException {
        YamlAnimalBreedingPolicyProvider provider = load("{}");

        for (AnimalKind animal : AnimalKind.values()) {
            assertTrue(provider.policyFor(animal).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[]",
        "villager: {minecraft:winter: 0.5}",
        "cow: 0.5",
        "cow: {winter: 0.5}",
        "cow: {minecraft:winter: '0.5'}",
        "cow: {minecraft:winter: 1.1}",
        "cow: {minecraft:winter: .nan}",
        "cow: {minecraft:winter: null}",
        "cow: {}\ncow: {}",
        "cow:\n  minecraft:winter: 0.5\n  minecraft:winter: 0.7"
    })
    void rejectsInvalidPoliciesAtConstruction(String yaml) throws IOException {
        Path policyFile = Files.writeString(tempDirectory.resolve("animal-breeding.yml"), yaml);

        assertThrows(IllegalArgumentException.class, () -> new YamlAnimalBreedingPolicyProvider(policyFile));
    }

    private YamlAnimalBreedingPolicyProvider load(String yaml) throws IOException {
        Path policyFile = Files.writeString(tempDirectory.resolve("animal-breeding.yml"), yaml);
        return new YamlAnimalBreedingPolicyProvider(policyFile);
    }
}
