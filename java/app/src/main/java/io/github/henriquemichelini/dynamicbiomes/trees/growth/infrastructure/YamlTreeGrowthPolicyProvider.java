package io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthChanceVariationSource;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.UnsupportedTreeGrowthPolicyException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlTreeGrowthPolicyProvider implements TreeGrowthPolicyProvider {
    private static final String BIOMES_KEY = "biomes";
    private static final String GROWTH_CHANCE_KEY = "growth-chance";
    private static final String SEASONAL_FACTORS_KEY = "seasonal-factors";

    private final Map<BiomeId, TreeGrowthPolicy> policies;

    public YamlTreeGrowthPolicyProvider(
        @NonNull Path policyFile,
        @NonNull TreeGrowthChanceVariationSource variationSource
    ) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            policies = parsePolicies(yaml.load(reader), variationSource);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read tree growth policy file: " + policyFile,
                exception
            );
        } catch (YAMLException | IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Invalid tree growth policy file " + policyFile + ": " + exception.getMessage(),
                exception
            );
        }
    }

    @Override
    public TreeGrowthPolicy policyFor(@NonNull BiomeId biomeId) {
        TreeGrowthPolicy policy = policies.get(biomeId);
        if (policy == null) {
            throw new UnsupportedTreeGrowthPolicyException(
                "Missing tree growth policy for biome: " + biomeId.value()
            );
        }
        return policy;
    }

    private static Map<BiomeId, TreeGrowthPolicy> parsePolicies(
        Object loaded,
        TreeGrowthChanceVariationSource variationSource
    ) {
        Map<?, ?> root = requiredMap(loaded, "tree growth policy root");
        rejectUnknownKeys(root, Set.of(BIOMES_KEY), "tree growth policy root");
        Map<?, ?> biomes = requiredMap(root.get(BIOMES_KEY), BIOMES_KEY);
        Map<BiomeId, TreeGrowthPolicy> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : biomes.entrySet()) {
            BiomeId biomeId = new BiomeId(requiredKey(entry.getKey(), BIOMES_KEY));
            String path = BIOMES_KEY + "." + biomeId.value();
            Map<?, ?> policy = requiredMap(entry.getValue(), path);
            rejectUnknownKeys(policy, Set.of(GROWTH_CHANCE_KEY, SEASONAL_FACTORS_KEY), path);

            double chance = requiredNumber(policy.get(GROWTH_CHANCE_KEY), path + "." + GROWTH_CHANCE_KEY);
            result.put(biomeId, new TreeGrowthPolicy(
                new TreeGrowthChance(chance),
                parseSeasonalFactors(policy, path),
                variationSource
            ));
        }
        return Map.copyOf(result);
    }

    private static Map<SeasonId, TreeGrowthSeasonalFactor> parseSeasonalFactors(
        Map<?, ?> policy,
        String policyPath
    ) {
        if (!policy.containsKey(SEASONAL_FACTORS_KEY)) {
            return Map.of();
        }

        String path = policyPath + "." + SEASONAL_FACTORS_KEY;
        Map<?, ?> factors = requiredMap(policy.get(SEASONAL_FACTORS_KEY), path);
        Map<SeasonId, TreeGrowthSeasonalFactor> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : factors.entrySet()) {
            SeasonId seasonId = new SeasonId(requiredKey(entry.getKey(), path));
            double factor = requiredNumber(entry.getValue(), path + "." + seasonId.value());
            result.put(seasonId, new TreeGrowthSeasonalFactor(factor));
        }
        return result;
    }

    private static Map<?, ?> requiredMap(Object value, String path) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be a mapping");
        }
        return map;
    }

    private static String requiredKey(Object key, String path) {
        if (!(key instanceof String stringKey)) {
            throw new IllegalArgumentException(path + " must use string keys");
        }
        return stringKey;
    }

    private static double requiredNumber(Object value, String path) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(path + " must be a number");
        }
        return number.doubleValue();
    }

    private static void rejectUnknownKeys(Map<?, ?> map, Set<String> supportedKeys, String path) {
        for (Object key : map.keySet()) {
            if (!supportedKeys.contains(requiredKey(key, path))) {
                throw new IllegalArgumentException("Unknown key in " + path + ": " + key);
            }
        }
    }
}
