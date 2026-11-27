package io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginPersistenceException;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.error.YAMLException;

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
    void diagnosticsAreReadOnlyAndTimeEachActualWriteAttemptOnce() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        Files.writeString(repositoryFile, "origins: invalid");
        AtomicLong time = new AtomicLong();
        YamlOreOriginRepository repository = new YamlOreOriginRepository(
            repositoryFile, () -> time.getAndAdd(3_000_000L)
        );

        var initial = repository.diagnostics();
        assertEquals(Duration.ofMillis(3), initial.window());
        assertEquals(0, initial.writeAttempts());
        assertEquals(Optional.empty(), initial.p95UpperBound());
        assertEquals(OptionalInt.empty(), initial.trackedOrigins());
        assertThrows(IllegalArgumentException.class, () -> repository.findByPosition(POSITION));
        assertEquals(0, repository.diagnostics().writeAttempts());
        Files.delete(repositoryFile);

        repository.removeByPosition(POSITION);
        assertTrue(repository.findByPosition(POSITION).isEmpty());
        assertFalse(Files.exists(repositoryFile));
        var loaded = repository.diagnostics();
        assertEquals(0, loaded.writeAttempts());
        assertEquals(OptionalInt.of(0), loaded.trackedOrigins());

        repository.save(PLAYER_PLACED_ORIGIN);
        var written = repository.diagnostics();
        assertEquals(1, written.writeAttempts());
        assertEquals(1, written.writeSuccesses());
        assertEquals(0, written.writeFailures());
        assertEquals(Optional.of(Duration.ofMillis(3)), written.p95UpperBound());
        assertEquals(OptionalInt.of(1), written.trackedOrigins());

        Files.move(repositoryFile, temporaryDirectory.resolve("previous-origins.yml"));
        Files.createDirectory(repositoryFile);
        OreOriginPersistenceException failure = assertThrows(
            OreOriginPersistenceException.class, () -> repository.removeByPosition(POSITION)
        );
        assertInstanceOf(IOException.class, failure.getCause());
        assertEquals(Optional.of(PLAYER_PLACED_ORIGIN), repository.findByPosition(POSITION));
        var failed = repository.diagnostics();
        assertEquals(2, failed.writeAttempts());
        assertEquals(1, failed.writeSuccesses());
        assertEquals(1, failed.writeFailures());
        assertEquals(Optional.of(Duration.ofMillis(3)), failed.p95UpperBound());
        assertEquals(OptionalInt.of(1), failed.trackedOrigins());
    }

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
    void appliesMovementAsARepositoryUpdate() {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);
        BlockPosition destination = new BlockPosition(POSITION.world(), POSITION.x() + 1, POSITION.y(), POSITION.z());
        OreOrigin movedOrigin = new OreOrigin(destination, OreOriginType.PLAYER_PLACED);
        repository.save(PLAYER_PLACED_ORIGIN);

        repository.applyChanges(List.of(POSITION, destination), List.of(movedOrigin));

        YamlOreOriginRepository reopened = new YamlOreOriginRepository(repositoryFile);
        assertTrue(repository.findByPosition(POSITION).isEmpty());
        assertTrue(reopened.findByPosition(POSITION).isEmpty());
        assertEquals(Optional.of(movedOrigin), repository.findByPosition(destination));
        assertEquals(Optional.of(movedOrigin), reopened.findByPosition(destination));
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
    void invalidPersistedDataStillFailsAfterReadingAValidEntry() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        Files.writeString(
            repositoryFile,
            """
            origins:
              - world: fdab89dd-8aac-4be0-9c26-8752ae6ce85e
                x: 11
                y: 64
                z: -20
                type: PLAYER_PLACED
              - world: not-a-uuid
                x: 10
                y: 64
                z: -20
                type: PLAYER_PLACED
            """
        );

        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> repository.findByPosition(POSITION)
        );

        assertTrue(exception.getMessage().contains("world"));
        assertThrows(IllegalArgumentException.class, () -> repository.findByPosition(POSITION));
        BlockPosition validPosition = new BlockPosition(POSITION.world(), 11, 64, -20);
        assertThrows(IllegalArgumentException.class, () -> repository.findByPosition(validPosition));
    }

    @Test
    void rejectsDuplicateOriginYamlKeys() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        Files.writeString(repositoryFile, """
            origins:
              - world: fdab89dd-8aac-4be0-9c26-8752ae6ce85e
                x: 10
                y: 64
                z: -20
                type: PLAYER_PLACED
                type: NATURAL
            """);

        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);

        assertThrows(YAMLException.class, () -> repository.findByPosition(POSITION));
    }

    @Test
    void failedWritesKeepTheLastPersistedOriginInMemory() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("ore-origins.yml");
        YamlOreOriginRepository repository = new YamlOreOriginRepository(repositoryFile);
        repository.save(PLAYER_PLACED_ORIGIN);
        Files.move(repositoryFile, temporaryDirectory.resolve("previous-origins.yml"));
        Files.createDirectory(repositoryFile);

        OreOrigin naturalOrigin = new OreOrigin(POSITION, OreOriginType.NATURAL);
        OreOriginPersistenceException failure =
            assertThrows(OreOriginPersistenceException.class, () -> repository.save(naturalOrigin));
        assertInstanceOf(IOException.class, failure.getCause());
        assertEquals(PLAYER_PLACED_ORIGIN, repository.findByPosition(POSITION).orElseThrow());
        assertThrows(OreOriginPersistenceException.class, () -> repository.removeByPosition(POSITION));
        assertEquals(PLAYER_PLACED_ORIGIN, repository.findByPosition(POSITION).orElseThrow());
    }
}
