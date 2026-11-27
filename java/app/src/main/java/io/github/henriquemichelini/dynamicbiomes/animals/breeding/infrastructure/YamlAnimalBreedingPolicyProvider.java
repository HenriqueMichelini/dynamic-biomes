package io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicy;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlAnimalBreedingPolicyProvider implements AnimalBreedingPolicyProvider {
    private final Map<AnimalKind, AnimalBreedingPolicy> policies;

    public YamlAnimalBreedingPolicyProvider(Path policyFile) {
        Map<AnimalKind, AnimalBreedingPolicy> loadedPolicies = new EnumMap<>(AnimalKind.class);
        for (Map.Entry<?, ?> entry : loadRoot(policyFile).entrySet()) {
            if (!(entry.getKey() instanceof String animalKey)) {
                throw new IllegalArgumentException("Animal breeding policy keys must be animal names");
            }
            AnimalKind animal = AnimalKind.fromPolicyKey(animalKey).orElseThrow(() ->
                new IllegalArgumentException("Unsupported animal breeding policy key: " + animalKey)
            );
            loadedPolicies.put(animal, parsePolicy(animalKey, entry.getValue()));
        }
        this.policies = Map.copyOf(loadedPolicies);
    }

    @Override
    public Optional<AnimalBreedingPolicy> policyFor(AnimalKind animal) {
        return Optional.ofNullable(policies.get(animal));
    }

    private static AnimalBreedingPolicy parsePolicy(String animalKey, Object value) {
        if (!(value instanceof Map<?, ?> seasonEntries)) {
            throw new IllegalArgumentException("Breeding policy for " + animalKey + " must be a season mapping");
        }
        Map<SeasonId, Double> chances = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : seasonEntries.entrySet()) {
            if (!(entry.getKey() instanceof String seasonKey) || !(entry.getValue() instanceof Number chance)) {
                throw new IllegalArgumentException(
                    "Breeding policy for " + animalKey + " requires namespaced season keys and numeric chances"
                );
            }
            chances.put(new SeasonId(seasonKey), chance.doubleValue());
        }
        return new AnimalBreedingPolicy(chances);
    }

    private static Map<?, ?> loadRoot(Path policyFile) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            if (yaml.load(reader) instanceof Map<?, ?> root) {
                return root;
            }
            throw new IllegalArgumentException("Animal breeding policy must be a mapping: " + policyFile);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read animal breeding policy: " + policyFile, exception);
        } catch (YAMLException exception) {
            throw new IllegalArgumentException("Invalid animal breeding policy YAML: " + policyFile, exception);
        }
    }
}
