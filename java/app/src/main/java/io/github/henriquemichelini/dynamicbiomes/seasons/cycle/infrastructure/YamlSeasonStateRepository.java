package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlSeasonStateRepository implements SeasonStateRepository {
    private static final String CURRENT_SEASON_KEY = "current-season";

    private final Path repositoryFile;

    public YamlSeasonStateRepository(Path repositoryFile) {
        this.repositoryFile = repositoryFile;
    }

    @Override
    public Optional<SeasonId> findCurrentSeason() {
        if (Files.notExists(repositoryFile)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(repositoryFile)) {
            Object loaded = yaml().load(reader);
            Map<?, ?> root = requiredMap(loaded);
            return Optional.of(new SeasonId(requiredCurrentSeason(root)));
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to read season state repository file: " + repositoryFile,
                exception
            );
        } catch (YAMLException exception) {
            throw new IllegalArgumentException(
                "Invalid season state repository YAML file: " + repositoryFile,
                exception
            );
        }
    }

    @Override
    public void saveCurrentSeason(SeasonId seasonId) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put(CURRENT_SEASON_KEY, seasonId.value());

        try {
            Path parent = repositoryFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(repositoryFile)) {
                yaml().dump(root, writer);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to write season state repository file: " + repositoryFile,
                exception
            );
        }
    }

    private static Map<?, ?> requiredMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid("repository root must be a mapping");
        }
        return map;
    }

    private static String requiredCurrentSeason(Map<?, ?> root) {
        Object value = root.get(CURRENT_SEASON_KEY);
        if (!(value instanceof String seasonId) || seasonId.isBlank()) {
            throw invalid(CURRENT_SEASON_KEY + " must be a non-blank string");
        }
        return seasonId;
    }

    private static Yaml yaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(
            new SafeConstructor(loaderOptions),
            new Representer(dumperOptions),
            dumperOptions,
            loaderOptions
        );
    }

    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("Invalid season state repository data: " + detail);
    }
}
