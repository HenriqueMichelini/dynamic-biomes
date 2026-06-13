package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlSeasonStateRepositoryTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");

    @TempDir
    Path temporaryDirectory;

    @Test
    void savesCurrentSeasonAndLoadsItFromNewRepositoryInstance() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("season-state.yml");

        new YamlSeasonStateRepository(repositoryFile).saveCurrentSeason(SPRING);

        assertEquals("current-season: minecraft:spring\n", Files.readString(repositoryFile));
        assertEquals(
            SPRING,
            new YamlSeasonStateRepository(repositoryFile).findCurrentSeason().orElseThrow()
        );
    }

    @Test
    void updatingCurrentSeasonOverwritesPreviousState() throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("season-state.yml");
        YamlSeasonStateRepository repository = new YamlSeasonStateRepository(repositoryFile);
        repository.saveCurrentSeason(SPRING);

        repository.saveCurrentSeason(SUMMER);

        assertEquals("current-season: minecraft:summer\n", Files.readString(repositoryFile));
        assertEquals(
            SUMMER,
            new YamlSeasonStateRepository(repositoryFile).findCurrentSeason().orElseThrow()
        );
    }

    @Test
    void absentYamlFileIsAnEmptyRepository() {
        Path repositoryFile = temporaryDirectory.resolve("missing/season-state.yml");

        assertFalse(Files.exists(repositoryFile));
        assertTrue(new YamlSeasonStateRepository(repositoryFile).findCurrentSeason().isEmpty());
    }

    @Test
    void malformedYamlFailsClearly() throws IOException {
        Path repositoryFile = writeState("current-season: [");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonStateRepository(repositoryFile).findCurrentSeason()
        );

        assertTrue(exception.getMessage().contains(repositoryFile.toString()));
    }

    @Test
    void missingCurrentSeasonFailsClearly() throws IOException {
        Path repositoryFile = writeState("other-state: minecraft:spring\n");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonStateRepository(repositoryFile).findCurrentSeason()
        );

        assertTrue(exception.getMessage().contains("current-season"));
    }

    @Test
    void blankCurrentSeasonFailsClearly() throws IOException {
        Path repositoryFile = writeState("current-season: ' '\n");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonStateRepository(repositoryFile).findCurrentSeason()
        );

        assertTrue(exception.getMessage().contains("current-season"));
    }

    @Test
    void invalidCurrentSeasonFailsThroughSeasonIdValidation() throws IOException {
        Path repositoryFile = writeState("current-season: spring\n");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonStateRepository(repositoryFile).findCurrentSeason()
        );

        assertTrue(exception.getMessage().contains("Season identity"));
    }

    private Path writeState(String yaml) throws IOException {
        Path repositoryFile = temporaryDirectory.resolve("season-state.yml");
        Files.writeString(repositoryFile, yaml);
        return repositoryFile;
    }
}
