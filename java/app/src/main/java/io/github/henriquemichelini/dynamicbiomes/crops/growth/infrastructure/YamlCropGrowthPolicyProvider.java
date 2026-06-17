package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChanceVariationSource;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
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

public final class YamlCropGrowthPolicyProvider
    implements CropGrowthPolicyProvider {

    private static final String BIOMES_KEY = "biomes";
    private static final String WHEAT_KEY = "wheat";
    private static final String GROWTH_CHANCE_KEY = "growth-chance";
    private static final String SEASONAL_FACTORS_KEY = "seasonal-factors";

    private final Path policyFile;
    private final CropGrowthChanceVariationSource variationSource;

    public YamlCropGrowthPolicyProvider(
        Path policyFile,
        CropGrowthChanceVariationSource variationSource
    ) {
        this.policyFile = policyFile;
        this.variationSource = variationSource;
    }

    @Override
    public CropGrowthPolicy policyFor(BiomeId biomeId) {
        Map<?, ?> root = loadRoot();
        Map<?, ?> biomes = requiredMap(root, BIOMES_KEY, "crop growth policy root.biomes");
        Map<BiomeId, CropGrowthPolicy> policies = new LinkedHashMap<>();

        for (Map.Entry<?, ?> biomeEntry : biomes.entrySet()) {
            BiomeId configuredBiomeId = new BiomeId(
                requiredKey(biomeEntry.getKey(), "biome policy key")
            );
            CropGrowthPolicy previous = policies.put(
                configuredBiomeId,
                parsePolicy(configuredBiomeId, biomeEntry.getValue())
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate wheat growth policy for biome: " +
                        configuredBiomeId.value()
                );
            }
        }

        CropGrowthPolicy policy = policies.get(biomeId);
        if (policy == null) {
            throw new UnsupportedCropGrowthPolicyException(
                "Missing wheat growth policy for biome: " + biomeId.value()
            );
        }
        return policy;
    }

    private CropGrowthPolicy parsePolicy(
        BiomeId biomeId,
        Object policyValue
    ) {
        String policyPath = "policy '" + biomeId.value() + "'";
        Map<?, ?> policy = requiredMapValue(policyValue, policyPath);
        rejectUnsupportedCropKeys(policy, policyPath);
        if (!policy.containsKey(WHEAT_KEY)) {
            throw new UnsupportedCropGrowthPolicyException(
                "Missing wheat growth policy for biome: " + biomeId.value()
            );
        }
        Map<?, ?> wheat = requiredMap(policy, WHEAT_KEY, policyPath + ".wheat");
        double chance = requiredNumber(
            wheat,
            GROWTH_CHANCE_KEY,
            policyPath + ".wheat.growth-chance"
        );
        Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors =
            parseSeasonalFactors(wheat, policyPath + ".wheat");
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            seasonalFactors,
            variationSource
        );
    }

    private static Map<SeasonId, CropGrowthSeasonalFactor> parseSeasonalFactors(
        Map<?, ?> wheat,
        String wheatPath
    ) {
        if (!wheat.containsKey(SEASONAL_FACTORS_KEY)) {
            return Map.of();
        }

        Map<?, ?> seasonalFactors = requiredMap(
            wheat,
            SEASONAL_FACTORS_KEY,
            wheatPath + ".seasonal-factors"
        );
        Map<SeasonId, CropGrowthSeasonalFactor> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : seasonalFactors.entrySet()) {
            SeasonId seasonId = new SeasonId(
                requiredKey(entry.getKey(), wheatPath + ".seasonal-factors key")
            );
            double factor = requiredNumberValue(
                entry.getValue(),
                wheatPath + ".seasonal-factors." + seasonId.value()
            );
            CropGrowthSeasonalFactor previous = result.put(
                seasonId,
                new CropGrowthSeasonalFactor(factor)
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate wheat growth seasonal factor for season: " +
                        seasonId.value()
                );
            }
        }
        return result;
    }

    private static void rejectUnsupportedCropKeys(
        Map<?, ?> policy,
        String policyPath
    ) {
        for (Object key : policy.keySet()) {
            if (!WHEAT_KEY.equals(key)) {
                throw new IllegalArgumentException(
                    "Unsupported crop growth policy key in " + policyPath + ": " + key
                );
            }
        }
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "crop growth policy root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read crop growth policy file: " + policyFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid crop growth policy YAML file: " + policyFile,
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
        Map<?, ?> parent,
        String key,
        String description
    ) {
        return requiredNumberValue(parent.get(key), description);
    }

    private static double requiredNumberValue(
        Object value,
        String description
    ) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " + description
            );
        }
        return number.doubleValue();
    }
}
