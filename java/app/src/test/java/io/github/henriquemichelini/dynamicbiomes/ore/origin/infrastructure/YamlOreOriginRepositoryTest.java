package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlOreOriginRepositoryTest {
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
        10,
        64,
        -20
    );
    private static final OreOrigin PLAYER_PLACED_ORIGIN = new OreOrigin(
        POSITION,
        OreOriginType.PLAYER_PLACED
    );

    @TempDir
    Path temporaryDirectory;

    @Test
    void savesPlayerPlacedOriginAndLoadsItFromNewRepositoryInstance() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");

        new YamlOreOriginRepository(repositoryFile).save(PLAYER_PLACED_ORIGIN);

        assertTrue(Files.readString(repositoryFile).contains("PLAYER_PLACED"));
        assertEquals(
            PLAYER_PLACED_ORIGIN,
            new YamlOreOriginRepository(repositoryFile)
                .findByPosition(POSITION)
                .orElseThrow()
        );
    }

    @Test
    void removingOriginDeletesItFromPersistedState() {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);
        repository.save(PLAYER_PLACED_ORIGIN);

        repository.removeByPosition(POSITION);

        assertTrue(new YamlOreOriginRepository(repositoryFile).findByPosition(POSITION).isEmpty());
    }

    @Test
    void unknownPositionReturnsEmpty() {
        YamlOreOriginRepository repository = new YamlOreOriginRepository(
            temporaryDirectory.resolve("ore-origins.yml")
        );

        assertTrue(repository.findByPosition(POSITION).isEmpty());
    }

    @Test
    void absentYamlFileIsAnEmptyRepository() {
        Path repositoryFile = temporaryDirectory.resolve("missing/ore-origins.yml");

        assertFalse(Files.exists(repositoryFile));
        assertTrue(new YamlOreOriginRepository(repositoryFile).findByPosition(POSITION).isEmpty());
    }

    @Test
    void savedOriginIsVisibleToSubsequentFindsOnSameInstance() {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);

        repository.save(PLAYER_PLACED_ORIGIN);

        assertEquals(
            PLAYER_PLACED_ORIGIN,
            repository.findByPosition(POSITION).orElseThrow()
        );
    }

    @Test
    void removedOriginIsNotVisibleToSubsequentFindsOnSameInstance() {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);
        repository.save(PLAYER_PLACED_ORIGIN);

        repository.removeByPosition(POSITION);

        assertTrue(repository.findByPosition(POSITION).isEmpty());
    }

    @Test
    void invalidPersistedDataFailsClearly() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        Files.writeString(
            repositoryFile,
            """
            origins:
              - world: not-a-uuid
                x: 10
                y: 64
                z: -20
                type: PLAYER_PLACED
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreOriginRepository(repositoryFile).findByPosition(POSITION)
        );

        assertTrue(exception.getMessage().contains("world"));
    }
}
