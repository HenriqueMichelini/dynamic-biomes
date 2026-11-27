package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCycleSettings;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class YamlSeasonCycleSettingsProvider {
    private static final String CALENDAR_KEY = "calendar";
    private static final String ADVANCEMENT_KEY = "advancement";
    private static final String ENABLED_KEY = "enabled";
    private static final String INITIAL_DELAY_KEY = "initial-delay-ticks";
    private static final String INTERVAL_KEY = "interval-ticks";

    private final Path settingsFile;

    public YamlSeasonCycleSettingsProvider(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public SeasonCycleSettings settings() {
        try (Reader reader = Files.newBufferedReader(settingsFile)) {
            Object loaded = yaml().load(reader);
            Map<?, ?> root = requiredMap(loaded, "settings root");
            Map<?, ?> advancement = requiredMap(
                root.get(ADVANCEMENT_KEY),
                ADVANCEMENT_KEY
            );

            return new SeasonCycleSettings(
                requiredBoolean(advancement, ENABLED_KEY),
                requiredLong(advancement, INITIAL_DELAY_KEY),
                requiredLong(advancement, INTERVAL_KEY),
                calendar(root)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to read season cycle settings file: " + settingsFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid season cycle settings YAML file: " + settingsFile,
                exception
            );
        }
    }

    private static SeasonCalendar calendar(Map<?, ?> root) {
        if (!root.containsKey(CALENDAR_KEY)) {
            return SeasonCalendar.standard();
        }
        if (!(root.get(CALENDAR_KEY) instanceof List<?> entries)) {
            throw invalid(CALENDAR_KEY + " must be a sequence of season identifiers");
        }
        List<SeasonId> seasons = new ArrayList<>(entries.size());
        for (Object entry : entries) {
            if (!(entry instanceof String value)) {
                throw invalid(CALENDAR_KEY + " entries must be season identifier strings");
            }
            seasons.add(new SeasonId(value));
        }
        return new SeasonCalendar(seasons);
    }

    private static Map<?, ?> requiredMap(Object value, String context) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid(context + " must be a mapping");
        }
        return map;
    }

    private static boolean requiredBoolean(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (!(value instanceof Boolean booleanValue)) {
            throw invalid(ADVANCEMENT_KEY + "." + key + " must be a boolean");
        }
        return booleanValue;
    }

    private static long requiredLong(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (!(value instanceof Integer || value instanceof Long)) {
            throw invalid(ADVANCEMENT_KEY + "." + key + " must be a long integer");
        }
        return ((Number) value).longValue();
    }

    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException(
            "Invalid season cycle settings: " + detail
        );
    }

    private static Yaml yaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(loaderOptions));
    }
}
