package io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlBiomeProfileProvider implements BiomeProfileProvider {
    private final Path profileFile;

    public YamlBiomeProfileProvider(Path profileFile) {
        this.profileFile = profileFile;
    }

    @Override
    public BiomeProfile profileFor(BiomeId biomeId) {
        Map<?, ?> root = loadRoot();
        Map<?, ?> profiles = requiredMap(root, "profiles", "biome profiles");
        if (!profiles.containsKey(biomeId.value())) {
            return null;
        }

        Map<?, ?> profile = requiredMapValue(
            profiles.get(biomeId.value()),
            "biome profile '" + biomeId.value() + "'"
        );
        Map<?, ?> climate = requiredMap(
            profile,
            "climate",
            "biome profile '" + biomeId.value() + "'.climate"
        );

        return new BiomeProfile(
            biomeId,
            new ClimateProfile(
                new Humidity(requiredNumber(climate, "humidity", biomeId)),
                new Temperature(requiredNumber(climate, "temperature", biomeId))
            ),
            new Fertility(requiredNumber(profile, "fertility", biomeId))
        );
    }

    private Map<?, ?> loadRoot() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));

        try (Reader reader = Files.newBufferedReader(profileFile)) {
            Object loaded = yaml.load(reader);
            return requiredMapValue(loaded, "biome profile root");
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                "Unable to read biome profile file: " + profileFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid biome profile YAML file: " + profileFile,
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

    private static double requiredNumber(
        Map<?, ?> profile,
        String field,
        BiomeId biomeId
    ) {
        Object value = profile.get(field);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                "Missing required " + field + " for biome profile '" + biomeId.value() + "'"
            );
        }
        return number.doubleValue();
    }
}
