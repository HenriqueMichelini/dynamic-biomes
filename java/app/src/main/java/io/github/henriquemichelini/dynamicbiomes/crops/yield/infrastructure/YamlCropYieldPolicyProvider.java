package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
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

public final class YamlCropYieldPolicyProvider
    implements CropYieldPolicyProvider {

    private static final String CROPS_KEY = "crops";
    private static final String MULTIPLIER_KEY = "multiplier";
    private static final String MIN_KEY = "min";
    private static final String MAX_KEY = "max";
    private static final String SEASONAL_FACTORS_KEY = "seasonal-factors";

    private final Path policyFile;
    private final Map<BiomeId, CropYieldPolicy> policies;

    public YamlCropYieldPolicyProvider(Path policyFile) {
        this.policyFile = policyFile;
        this.policies = parsePolicies(loadRoot());
    }

    @Override
    public CropYieldPolicy policyFor(BiomeId biomeId) {
        CropYieldPolicy policy = policies.get(biomeId);
        if (policy == null) {
            throw new UnsupportedCropYieldPolicyException(
                "Missing crop yield policy for biome: " + biomeId.value()
            );
        }
        return policy;
    }

    private static Map<BiomeId, CropYieldPolicy> parsePolicies(Map<?, ?> root) {
        Map<BiomeId, CropYieldPolicy> policies = new LinkedHashMap<>();

        for (Map.Entry<?, ?> biomeEntry : root.entrySet()) {
            String biomeKey = requiredKey(
                biomeEntry.getKey(),
                "crop yield policy root biome key"
            );
            BiomeId biomeId = new BiomeId(biomeKey);
            String biomePath = "crop yield policy root." + biomeId.value();
            Map<?, ?> biomePolicy = requiredMapValue(
                biomeEntry.getValue(),
                biomePath
            );
            Map<?, ?> crops = requiredMap(
                biomePolicy,
                CROPS_KEY,
                biomePath + ".crops"
            );
            rejectUnsupportedCropKeys(crops, biomePath + ".crops");
            CropYieldPolicy previous = policies.put(
                biomeId,
                parsePolicy(biomeId, crops, biomePath + ".crops")
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate crop yield policy for biome: " + biomeId.value()
                );
            }
        }

        return Map.copyOf(policies);
    }

    private static CropYieldPolicy parsePolicy(
        BiomeId biomeId,
        Map<?, ?> crops,
        String policyPath
    ) {
        Map<CropKind, CropYieldCropRule> rules = new LinkedHashMap<>();

        for (Map.Entry<?, ?> cropEntry : crops.entrySet()) {
            String cropKey = requiredKey(cropEntry.getKey(), policyPath + " crop key");
            CropKind cropKind = CropKind.fromPolicyKey(cropKey)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unsupported crop yield policy key in " + policyPath + ": " + cropKey
                ));
            Map<?, ?> cropPolicy = requiredMapValue(
                cropEntry.getValue(),
                policyPath + "." + cropKind.policyKey()
            );
            CropYieldCropRule previous = rules.put(
                cropKind,
                new CropYieldCropRule(
                    parseMultiplierRange(cropPolicy, policyPath, cropKind),
                    parseSeasonalFactors(cropPolicy, policyPath, cropKind)
                )
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate crop yield rule for crop kind: " + cropKind.policyKey()
                );
            }
        }

        return new CropYieldPolicy(biomeId, rules);
    }

    private static CropYieldMultiplierRange parseMultiplierRange(
        Map<?, ?> cropPolicy,
        String policyPath,
        CropKind cropKind
    ) {
        String cropPath = policyPath + "." + cropKind.policyKey();
        Map<?, ?> multiplier = requiredMap(
            cropPolicy,
            MULTIPLIER_KEY,
            cropPath + ".multiplier"
        );
        double minimum = requiredNumber(
            multiplier,
            MIN_KEY,
            cropPath + ".multiplier.min"
        );
        double maximum = requiredNumber(
            multiplier,
            MAX_KEY,
            cropPath + ".multiplier.max"
        );
        return new CropYieldMultiplierRange(minimum, maximum);
    }

    private static Map<SeasonId, CropYieldSeasonalFactor> parseSeasonalFactors(
        Map<?, ?> cropPolicy,
        String policyPath,
        CropKind cropKind
    ) {
        if (!cropPolicy.containsKey(SEASONAL_FACTORS_KEY)) {
            return Map.of();
        }

        String cropPath = policyPath + "." + cropKind.policyKey();
        Map<?, ?> seasonalFactors = requiredMap(
            cropPolicy,
            SEASONAL_FACTORS_KEY,
            cropPath + ".seasonal-factors"
        );
        Map<SeasonId, CropYieldSeasonalFactor> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : seasonalFactors.entrySet()) {
            SeasonId seasonId = new SeasonId(
                requiredKey(entry.getKey(), cropPath + ".seasonal-factors key")
            );
            double factor = requiredNumberValue(
                entry.getValue(),
                cropPath + ".seasonal-factors." + seasonId.value()
            );
            CropYieldSeasonalFactor previous = result.put(
                seasonId,
                new CropYieldSeasonalFactor(factor)
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate crop yield seasonal factor for season: " +
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
                    "Unsupported crop yield policy key in " + policyPath + ": " + key
                );
            }
        }
    }

    private static boolean isSupportedCropKey(Object key) {
        return key instanceof String stringKey &&
            CropKind.fromPolicyKey(stringKey).isPresent();
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (Reader reader = Files.newBufferedReader(policyFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "crop yield policy root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read crop yield policy file: " + policyFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid crop yield policy YAML file: " + policyFile,
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
