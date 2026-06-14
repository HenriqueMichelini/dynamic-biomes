package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCycleSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlSeasonCycleSettingsProviderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsDisabledSettings() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: false
              initial-delay-ticks: 24000
              interval-ticks: 24000
            """
        );

        SeasonCycleSettings settings = new YamlSeasonCycleSettingsProvider(
            settingsFile
        ).settings();

        assertFalse(settings.enabled());
        assertEquals(24000L, settings.initialDelayTicks());
        assertEquals(24000L, settings.intervalTicks());
    }

    @Test
    void loadsEnabledSettings() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: true
              initial-delay-ticks: 1200
              interval-ticks: 72000
            """
        );

        SeasonCycleSettings settings = new YamlSeasonCycleSettingsProvider(
            settingsFile
        ).settings();

        assertTrue(settings.enabled());
        assertEquals(1200L, settings.initialDelayTicks());
        assertEquals(72000L, settings.intervalTicks());
    }

    @Test
    void failsWhenFileIsMissing() {
        Path settingsFile = temporaryDirectory.resolve("missing.yml");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(exception.getMessage().contains("Unable to read"));
    }

    @Test
    void failsWhenAdvancementIsNotAMapping() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement: not-a-mapping
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(exception.getMessage().contains("advancement must be a mapping"));
    }

    @Test
    void failsWhenEnabledIsNotBoolean() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: "true"
              initial-delay-ticks: 0
              interval-ticks: 1
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(exception.getMessage().contains("advancement.enabled must be a boolean"));
    }

    @Test
    void failsWhenDelayIsNegative() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: false
              initial-delay-ticks: -1
              interval-ticks: 1
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(
            exception.getMessage().contains("initial-delay-ticks must be non-negative")
        );
    }

    @Test
    void failsWhenIntervalIsZero() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: false
              initial-delay-ticks: 0
              interval-ticks: 0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(
            exception.getMessage().contains("interval-ticks must be positive")
        );
    }

    @Test
    void failsWhenIntervalIsNotNumeric() throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: false
              initial-delay-ticks: 0
              interval-ticks: "not-a-number"
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings()
        );

        assertTrue(
            exception.getMessage().contains("advancement.interval-ticks must be a long integer")
        );
    }

    private Path writeFile(String content) throws IOException {
        Path settingsFile = temporaryDirectory.resolve("season-cycle.yml");
        Files.writeString(settingsFile, content);
        return settingsFile;
    }
}
