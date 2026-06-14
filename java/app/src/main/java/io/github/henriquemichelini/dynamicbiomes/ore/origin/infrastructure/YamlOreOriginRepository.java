package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class YamlOreOriginRepository implements OreOriginRepository {

    private static final String ORIGINS_KEY = "origins";

    private final Path repositoryFile;
    private Map<BlockPosition, OreOrigin> origins;

    public YamlOreOriginRepository(Path repositoryFile) {
        this.repositoryFile = repositoryFile;
    }

    @Override
    public void save(OreOrigin origin) {
        ensureLoaded();
        origins.put(origin.position(), origin);
        writeOrigins();
    }

    @Override
    public Optional<OreOrigin> findByPosition(BlockPosition position) {
        ensureLoaded();
        return Optional.ofNullable(origins.get(position));
    }

    @Override
    public void removeByPosition(BlockPosition position) {
        ensureLoaded();
        if (origins.remove(position) != null) {
            writeOrigins();
        }
    }

    private void ensureLoaded() {
        if (origins != null) {
            return;
        }

        if (Files.notExists(repositoryFile)) {
            origins = new LinkedHashMap<>();
            return;
        }

        try (Reader reader = Files.newBufferedReader(repositoryFile)) {
            Object loaded = yaml().load(reader);
            Map<?, ?> root = requiredMap(loaded, "repository root");
            List<?> entries = requiredList(root.get(ORIGINS_KEY), ORIGINS_KEY);
            origins = new LinkedHashMap<>();

            for (int index = 0; index < entries.size(); index++) {
                OreOrigin origin = parseOrigin(
                    requiredMap(entries.get(index), "origins[" + index + "]"),
                    index
                );
                if (origins.put(origin.position(), origin) != null) {
                    throw invalid(
                        "duplicate position at origins[" + index + "]"
                    );
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Unable to read ore origin repository file: " + repositoryFile,
                exception
            );
        }
    }

    private OreOrigin parseOrigin(Map<?, ?> entry, int index) {
        String context = "origins[" + index + "]";
        UUID worldId;
        try {
            worldId = UUID.fromString(requiredString(entry, "world", context));
        } catch (IllegalArgumentException exception) {
            throw invalid(context + ".world must be a valid UUID", exception);
        }

        BlockPosition position = new BlockPosition(
            new WorldReference(worldId),
            requiredInteger(entry, "x", context),
            requiredInteger(entry, "y", context),
            requiredInteger(entry, "z", context)
        );
        OreOriginType type;
        try {
            type = OreOriginType.valueOf(
                requiredString(entry, "type", context)
            );
        } catch (IllegalArgumentException exception) {
            throw invalid(
                context + ".type must be a valid ore origin type",
                exception
            );
        }
        return new OreOrigin(position, type);
    }

    private void writeOrigins() {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (OreOrigin origin : origins.values()) {
            BlockPosition position = origin.position();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world", position.world().id().toString());
            entry.put("x", position.x());
            entry.put("y", position.y());
            entry.put("z", position.z());
            entry.put("type", origin.type().name());
            entries.add(entry);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put(ORIGINS_KEY, entries);
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
                "Unable to write ore origin repository file: " + repositoryFile,
                exception
            );
        }
    }

    private static Yaml yaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(
            new SafeConstructor(loaderOptions),
            new Representer(options),
            options,
            loaderOptions
        );
    }

    private static Map<?, ?> requiredMap(Object value, String context) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid(context + " must be a mapping");
        }
        return map;
    }

    private static List<?> requiredList(Object value, String context) {
        if (!(value instanceof List<?> list)) {
            throw invalid(context + " must be a sequence");
        }
        return list;
    }

    private static String requiredString(
        Map<?, ?> entry,
        String key,
        String context
    ) {
        Object value = entry.get(key);
        if (!(value instanceof String string) || string.isBlank()) {
            throw invalid(context + "." + key + " must be a non-blank string");
        }
        return string;
    }

    private static int requiredInteger(
        Map<?, ?> entry,
        String key,
        String context
    ) {
        Object value = entry.get(key);
        if (!(value instanceof Number number)) {
            throw invalid(context + "." + key + " must be an integer");
        }
        long longValue = number.longValue();
        if (
            longValue < Integer.MIN_VALUE ||
            longValue > Integer.MAX_VALUE ||
            number.doubleValue() != longValue
        ) {
            throw invalid(context + "." + key + " must be an integer");
        }
        return (int) longValue;
    }

    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException(
            "Invalid ore origin repository data: " + detail
        );
    }

    private static IllegalArgumentException invalid(
        String detail,
        Exception cause
    ) {
        return new IllegalArgumentException(
            "Invalid ore origin repository data: " + detail,
            cause
        );
    }
}
