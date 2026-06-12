package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class YamlOreDropPolicyProvider implements OreDropPolicyProvider {
    private final Path policyFile;

    public YamlOreDropPolicyProvider(Path policyFile) {
        this.policyFile = policyFile;
    }

    @Override
    public OreDropPolicy policyFor(String policyKey) {
        Map<?, ?> root = loadRoot();
        Map<?, ?> policy = requiredMap(root, policyKey, "policy '" + policyKey + "'");
        Map<?, ?> ores = requiredMap(policy, "ores", "policy '" + policyKey + "'.ores");
        Map<String, OreDropMultiplierRange> ranges = new LinkedHashMap<>();

        for (Map.Entry<?, ?> oreEntry : ores.entrySet()) {
            String oreKey = requiredKey(oreEntry.getKey(), "ore key");
            Map<?, ?> range = requiredMapValue(
                oreEntry.getValue(),
                "policy '" + policyKey + "'.ores." + oreKey
            );
            double minimum = requiredNumber(range, "min", policyKey, oreKey);
            double maximum = requiredNumber(range, "max", policyKey, oreKey);
            ranges.put(oreKey, new OreDropMultiplierRange(minimum, maximum));
        }

        return new OreDropPolicy(policyKey, ranges);
    }

    private Map<?, ?> loadRoot() {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "ore drop policy root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read ore drop policy file: " + policyFile,
                exception
            );
        }
    }

    private static Map<?, ?> requiredMap(Map<?, ?> parent, String key, String description) {
        if (!parent.containsKey(key)) {
            throw new IllegalArgumentException("Missing required " + description);
        }
        return requiredMapValue(parent.get(key), description);
    }

    private static Map<?, ?> requiredMapValue(Object value, String description) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Required " + description + " must be a mapping");
        }
        return map;
    }

    private static String requiredKey(Object key, String description) {
        if (!(key instanceof String stringKey) || stringKey.isBlank()) {
            throw new IllegalArgumentException("Required " + description + " must not be blank");
        }
        return stringKey;
    }

    private static double requiredNumber(
        Map<?, ?> range,
        String bound,
        String policyKey,
        String oreKey
    ) {
        Object value = range.get(bound);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " + bound + " for policy '"
                    + policyKey + "'.ores." + oreKey
            );
        }
        return number.doubleValue();
    }
}
