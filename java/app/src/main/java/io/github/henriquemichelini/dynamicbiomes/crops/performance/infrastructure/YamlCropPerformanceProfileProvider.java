package io.github.henriquemichelini.dynamicbiomes.crops.performance.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NonNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlCropPerformanceProfileProvider
    implements CropPerformanceProfileProvider
{

    private static final String PROFILES_KEY = "profiles";
    private static final String PREFERENCES_KEY = "preferences";
    private static final String WIND_SPEED_KEY = "wind-speed";
    private static final String RAIN_STRENGTH_KEY = "rain-strength";
    private static final String HUMIDITY_KEY = "humidity";
    private static final String TEMPERATURE_KEY = "temperature";
    private static final String SOLAR_INCIDENCE_KEY = "solar-incidence";
    private static final String SOIL_FERTILITY_KEY = "soil-fertility";

    private final Path profileFile;
    private final Map<CropKind, CropPerformanceProfile> profiles;

    public YamlCropPerformanceProfileProvider(@NonNull Path profileFile) {
        this.profileFile = profileFile;
        this.profiles = parseProfiles(loadRoot());
    }

    @Override
    public CropPerformanceProfile profileFor(@NonNull CropKind cropKind) {
        CropPerformanceProfile profile = profiles.get(cropKind);
        if (profile == null) {
            throw new UnsupportedCropPerformanceProfileException(
                "Missing crop performance profile for crop: " +
                    cropKind.policyKey()
            );
        }
        return profile;
    }

    private static Map<CropKind, CropPerformanceProfile> parseProfiles(
        Map<?, ?> root
    ) {
        Map<?, ?> configuredProfiles = requiredMap(
            root,
            PROFILES_KEY,
            "crop performance profile root.profiles"
        );
        Map<CropKind, CropPerformanceProfile> profiles = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : configuredProfiles.entrySet()) {
            String cropKey = requiredKey(
                entry.getKey(),
                "crop performance profile crop key"
            );
            CropKind cropKind = CropKind.fromPolicyKey(cropKey).orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unsupported crop performance profile key: " + cropKey
                    )
            );
            Map<?, ?> profile = requiredMapValue(
                entry.getValue(),
                "crop performance profile '" + cropKind.policyKey() + "'"
            );
            CropPerformanceProfile previous = profiles.put(
                cropKind,
                parseProfile(cropKind, profile)
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate crop performance profile for crop: " +
                        cropKind.policyKey()
                );
            }
        }

        return Map.copyOf(profiles);
    }

    private static CropPerformanceProfile parseProfile(
        CropKind cropKind,
        Map<?, ?> profile
    ) {
        String profilePath =
            "crop performance profile '" + cropKind.policyKey() + "'";
        Map<?, ?> preferences = requiredMap(
            profile,
            PREFERENCES_KEY,
            profilePath + ".preferences"
        );

        return new CropPerformanceProfile(
            cropKind,
            preference(
                preferences,
                WIND_SPEED_KEY,
                profilePath + ".preferences.wind-speed"
            ),
            preference(
                preferences,
                RAIN_STRENGTH_KEY,
                profilePath + ".preferences.rain-strength"
            ),
            preference(
                preferences,
                HUMIDITY_KEY,
                profilePath + ".preferences.humidity"
            ),
            preference(
                preferences,
                TEMPERATURE_KEY,
                profilePath + ".preferences.temperature"
            ),
            preference(
                preferences,
                SOLAR_INCIDENCE_KEY,
                profilePath + ".preferences.solar-incidence"
            ),
            preference(
                preferences,
                SOIL_FERTILITY_KEY,
                profilePath + ".preferences.soil-fertility"
            )
        );
    }

    private static NormalizedEnvironmentalValue preference(
        Map<?, ?> parent,
        String key,
        String description
    ) {
        return new NormalizedEnvironmentalValue(
            requiredNumber(parent, key, description)
        );
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        try (Reader reader = Files.newBufferedReader(profileFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "crop performance profile root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read crop performance profile file: " + profileFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid crop performance profile YAML file: " + profileFile,
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
        Object value = parent.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " + description
            );
        }
        return number.doubleValue();
    }
}
