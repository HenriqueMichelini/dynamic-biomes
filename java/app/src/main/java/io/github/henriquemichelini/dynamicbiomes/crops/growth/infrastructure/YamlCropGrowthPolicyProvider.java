package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChanceVariationSource;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlCropGrowthPolicyProvider
    implements CropGrowthPolicyProvider {

    private static final String BIOMES_KEY = "biomes";
    private static final String CROPS_KEY = "crops";
    private static final String GROWTH_CHANCE_KEY = "growth-chance";
    private static final String SEASONAL_FACTORS_KEY = "seasonal-factors";

    private final Map<CropKind, CropGrowthPolicy> defaults;
    private final Map<BiomeId, Map<CropKind, CropGrowthPolicy>> biomePolicies;

    public YamlCropGrowthPolicyProvider(
        Path policyFile,
        CropGrowthChanceVariationSource variationSource
    ) {
        Map<?, ?> root = loadRoot(policyFile);
        if (!root.containsKey(CROPS_KEY) && !root.containsKey(BIOMES_KEY)) {
            throw new IllegalArgumentException("Crop growth policy must define crops or biomes");
        }
        rejectUnknownKeys(root, Set.of(CROPS_KEY, BIOMES_KEY), "crop growth policy root");
        this.defaults = root.containsKey(CROPS_KEY)
            ? parseCropPolicies(
                requiredMap(root, CROPS_KEY, "crop growth policy root.crops"),
                "crop growth policy root.crops",
                variationSource
            )
            : Map.of();
        this.biomePolicies = root.containsKey(BIOMES_KEY)
            ? parseBiomePolicies(
                requiredMap(root, BIOMES_KEY, "crop growth policy root.biomes"),
                variationSource
            )
            : Map.of();
    }

    @Override
    public CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind) {
        CropGrowthPolicy policy = biomePolicies.getOrDefault(biomeId, Map.of()).get(cropKind);
        if (policy == null) {
            policy = defaults.get(cropKind);
        }
        if (policy == null) {
            throw new UnsupportedCropGrowthPolicyException(
                "Missing " + cropKind.policyKey() + " growth policy for biome: " + biomeId.value()
            );
        }
        return policy;
    }

    private static Map<BiomeId, Map<CropKind, CropGrowthPolicy>> parseBiomePolicies(
        Map<?, ?> biomes,
        CropGrowthChanceVariationSource variationSource
    ) {
        Map<BiomeId, Map<CropKind, CropGrowthPolicy>> policies = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : biomes.entrySet()) {
            BiomeId biomeId = new BiomeId(requiredKey(entry.getKey(), "biome policy key"));
            String path = "policy '" + biomeId.value() + "'";
            policies.put(
                biomeId,
                parseCropPolicies(requiredMapValue(entry.getValue(), path), path, variationSource)
            );
        }
        return Map.copyOf(policies);
    }

    private static Map<CropKind, CropGrowthPolicy> parseCropPolicies(
        Map<?, ?> crops,
        String policyPath,
        CropGrowthChanceVariationSource variationSource
    ) {
        Map<CropKind, CropGrowthPolicy> policies = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : crops.entrySet()) {
            String cropKey = requiredKey(entry.getKey(), policyPath + " crop key");
            CropKind cropKind = CropKind.fromPolicyKey(cropKey).orElseThrow(() ->
                new IllegalArgumentException(
                    "Unsupported crop growth policy key in " + policyPath + ": " + cropKey
                )
            );
            String cropPath = policyPath + "." + cropKey;
            Map<?, ?> cropPolicy = requiredMapValue(entry.getValue(), cropPath);
            rejectUnknownKeys(cropPolicy, Set.of(GROWTH_CHANCE_KEY, SEASONAL_FACTORS_KEY), cropPath);
            CropGrowthChance chance = new CropGrowthChance(
                requiredNumberValue(
                    cropPolicy.get(GROWTH_CHANCE_KEY),
                    cropPath + ".growth-chance"
                )
            );
            policies.put(
                cropKind,
                new CropGrowthPolicy(
                    chance,
                    parseSeasonalFactors(cropPolicy, cropPath),
                    variationSource
                )
            );
        }
        return Map.copyOf(policies);
    }

    private static Map<SeasonId, CropGrowthSeasonalFactor> parseSeasonalFactors(
        Map<?, ?> cropPolicy,
        String cropPolicyPath
    ) {
        if (!cropPolicy.containsKey(SEASONAL_FACTORS_KEY)) {
            return Map.of();
        }

        Map<?, ?> seasonalFactors = requiredMap(
            cropPolicy,
            SEASONAL_FACTORS_KEY,
            cropPolicyPath + ".seasonal-factors"
        );
        Map<SeasonId, CropGrowthSeasonalFactor> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : seasonalFactors.entrySet()) {
            SeasonId seasonId = new SeasonId(
                requiredKey(entry.getKey(), cropPolicyPath + ".seasonal-factors key")
            );
            double factor = requiredNumberValue(
                entry.getValue(),
                cropPolicyPath + ".seasonal-factors." + seasonId.value()
            );
            CropGrowthSeasonalFactor previous = result.put(
                seasonId,
                new CropGrowthSeasonalFactor(factor)
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate crop growth seasonal factor for season: " +
                        seasonId.value()
                );
            }
        }
        return result;
    }

    private static Map<?, ?> loadRoot(Path policyFile) {
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

    private static void rejectUnknownKeys(Map<?, ?> fields, Set<String> supportedKeys, String path) {
        for (Object key : fields.keySet()) {
            if (!(key instanceof String name) || !supportedKeys.contains(name)) {
                throw new IllegalArgumentException("Unknown key in " + path + ": " + key);
            }
        }
    }

    private static String requiredKey(Object key, String description) {
        if (!(key instanceof String stringKey) || stringKey.isBlank()) {
            throw new IllegalArgumentException(
                "Required " + description + " must not be blank"
            );
        }
        return stringKey;
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
