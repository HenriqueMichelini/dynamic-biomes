package io.github.henriquemichelini.dynamicbiomes.seasons.profile.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
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

public final class YamlSeasonProfileProvider implements SeasonProfileProvider {
    private final Path profileFile;

    public YamlSeasonProfileProvider(Path profileFile) {
        this.profileFile = profileFile;
    }

    @Override
    public SeasonProfile profileFor(SeasonId seasonId) {
        Map<?, ?> root = loadRoot();
        Map<?, ?> configuredProfiles = requiredMap(root, "profiles", "season profiles");
        Map<SeasonId, SeasonProfile> profiles = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : configuredProfiles.entrySet()) {
            SeasonId configuredSeasonId = new SeasonId(
                requiredKey(entry.getKey(), "season profile key")
            );
            SeasonProfile previous = profiles.put(
                configuredSeasonId,
                parseProfile(configuredSeasonId, entry.getValue())
            );
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Duplicate season profile: " + configuredSeasonId.value()
                );
            }
        }

        SeasonProfile profile = profiles.get(seasonId);
        if (profile == null) {
            throw new IllegalArgumentException(
                "Missing season profile: " + seasonId.value()
            );
        }
        return profile;
    }

    private static SeasonProfile parseProfile(SeasonId seasonId, Object profileValue) {
        String description = "season profile '" + seasonId.value() + "'";
        Map<?, ?> profile = requiredMapValue(profileValue, description);
        Map<?, ?> climate = requiredMap(
            profile,
            "climate-adjustment",
            description + ".climate-adjustment"
        );
        return new SeasonProfile(
            seasonId,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(requiredNumber(climate, "temperature", description)),
                new SeasonalAdjustment(requiredNumber(climate, "humidity", description))
            )
        );
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

        try (Reader reader = Files.newBufferedReader(profileFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "season profile root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read season profile file: " + profileFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid season profile YAML file: " + profileFile,
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
        Map<?, ?> profile,
        String field,
        String profileDescription
    ) {
        Object value = profile.get(field);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " + field + " for " + profileDescription
            );
        }
        return number.doubleValue();
    }
}
