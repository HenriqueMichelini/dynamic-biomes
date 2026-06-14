package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlOreDropPolicyProvider implements OreDropPolicyProvider {

    private final Path policyFile;

    public YamlOreDropPolicyProvider(Path policyFile) {
        this.policyFile = policyFile;
    }

    @Override
    public OreDropPolicy policyFor(BiomeId biomeId) {
        Map<?, ?> root = loadRoot();
        Map<BiomeId, OreDropPolicy> policies = new LinkedHashMap<>();

        for (Map.Entry<?, ?> policyEntry : root.entrySet()) {
            BiomeId configuredBiomeId = new BiomeId(
                requiredKey(policyEntry.getKey(), "biome policy key")
            );
            OreDropPolicy previous = policies.put(
                configuredBiomeId,
                parsePolicy(configuredBiomeId, policyEntry.getValue())
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate ore drop policy for biome: " +
                        configuredBiomeId.value()
                );
            }
        }

        OreDropPolicy policy = policies.get(biomeId);
        if (policy == null) {
            throw new UnsupportedOreDropConfigurationException(
                "Missing ore drop policy for biome: " + biomeId.value()
            );
        }
        return policy;
    }

    private static OreDropPolicy parsePolicy(
        BiomeId biomeId,
        Object policyValue
    ) {
        String policyDescription = "policy '" + biomeId.value() + "'";
        Map<?, ?> policy = requiredMapValue(policyValue, policyDescription);
        Map<?, ?> ores = requiredMap(
            policy,
            "ores",
            policyDescription + ".ores"
        );
        Map<OreKind, OreDropMultiplierRange> ranges = new LinkedHashMap<>();

        for (Map.Entry<?, ?> oreEntry : ores.entrySet()) {
            OreKind oreKind = new OreKind(
                requiredKey(oreEntry.getKey(), "ore kind key")
            );
            Map<?, ?> range = requiredMapValue(
                oreEntry.getValue(),
                policyDescription + ".ores." + oreKind.value()
            );
            double minimum = requiredNumber(range, "min", biomeId, oreKind);
            double maximum = requiredNumber(range, "max", biomeId, oreKind);
            OreDropMultiplierRange previous = ranges.put(
                oreKind,
                new OreDropMultiplierRange(minimum, maximum)
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate ore drop multiplier range for ore kind: " +
                        oreKind.value()
                );
            }
        }

        return new OreDropPolicy(biomeId, ranges);
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "ore drop policy root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read ore drop policy file: " + policyFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid ore drop policy YAML file: " + policyFile,
                exception
            );
        }
    }

    private static Map<?, ?> requiredMap(
        Map<?, ?> parent,
        String key,
        String description
    ) {
        if (!parent.containsKey(key)) {
            throw new IllegalArgumentException(
                "Missing required " + description
            );
        }
        return requiredMapValue(parent.get(key), description);
    }

    private static Map<?, ?> requiredMapValue(
        Object value,
        String description
    ) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                "Required " + description + " must be a mapping"
            );
        }
        return map;
    }

    private static String requiredKey(Object key, String description) {
        if (!(key instanceof String stringKey) || stringKey.isBlank()) {
            throw new IllegalArgumentException(
                "Required " + description + " must not be blank"
            );
        }
        return stringKey;
    }

    private static double requiredNumber(
        Map<?, ?> range,
        String bound,
        BiomeId biomeId,
        OreKind oreKind
    ) {
        Object value = range.get(bound);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " +
                    bound +
                    " for policy '" +
                    biomeId.value() +
                    "'.ores." +
                    oreKind.value()
            );
        }
        return number.doubleValue();
    }
}
