package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropKind;
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
    public CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind) {
        Map<?, ?> root = loadRoot();
        Map<?, ?> biomes = requiredMap(root, BIOMES_KEY, "crop growth policy root.biomes");

        for (Map.Entry<?, ?> biomeEntry : biomes.entrySet()) {
            BiomeId configuredBiomeId = new BiomeId(
                requiredKey(biomeEntry.getKey(), "biome policy key")
            );
            Map<?, ?> policy = requiredMapValue(
                biomeEntry.getValue(),
                "policy '" + configuredBiomeId.value() + "'"
            );
            rejectUnsupportedCropKeys(
                policy,
                "policy '" + configuredBiomeId.value() + "'"
            );
            if (configuredBiomeId.equals(biomeId)) {
                return parsePolicy(configuredBiomeId, policy, cropKind);
            }
        }

        throw new UnsupportedCropGrowthPolicyException(
            "Missing " +
                cropKind.policyKey() +
                " growth policy for biome: " +
                biomeId.value()
        );
    }

    private CropGrowthPolicy parsePolicy(
        BiomeId biomeId,
        Map<?, ?> policy,
        CropKind cropKind
    ) {
        String policyPath = "policy '" + biomeId.value() + "'";
        if (!policy.containsKey(cropKind.policyKey())) {
            throw new UnsupportedCropGrowthPolicyException(
                "Missing " +
                    cropKind.policyKey() +
                    " growth policy for biome: " +
                    biomeId.value()
            );
        }
        Map<?, ?> cropPolicy = requiredMap(
            policy,
            cropKind.policyKey(),
            policyPath + "." + cropKind.policyKey()
        );
        double chance = requiredNumber(
            cropPolicy,
            GROWTH_CHANCE_KEY,
            policyPath + "." + cropKind.policyKey() + ".growth-chance"
        );
        Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors =
            parseSeasonalFactors(cropPolicy, policyPath + "." + cropKind.policyKey());
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            seasonalFactors,
            variationSource
        );
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

    private static void rejectUnsupportedCropKeys(
        Map<?, ?> policy,
        String policyPath
    ) {
        for (Object key : policy.keySet()) {
            if (!isSupportedCropKey(key)) {
                throw new IllegalArgumentException(
                    "Unsupported crop growth policy key in " + policyPath + ": " + key
                );
            }
        }
    }

    private static boolean isSupportedCropKey(Object key) {
        for (CropKind cropKind : CropKind.values()) {
            if (cropKind.policyKey().equals(key)) {
                return true;
            }
        }
        return false;
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
