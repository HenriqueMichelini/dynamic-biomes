package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCycleSettings;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        assertEquals(List.of(
            new SeasonId("minecraft:spring"),
            new SeasonId("minecraft:summer"),
            new SeasonId("minecraft:autumn"),
            new SeasonId("minecraft:winter")
        ), settings.calendar().seasons());
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

    @Test
    void usesTheCalendarOrderFromTheFile() throws IOException {
        Path settingsFile = writeFile(
            """
            calendar:
              - custom:dry
              - custom:rainy
            advancement:
              enabled: true
              initial-delay-ticks: 0
              interval-ticks: 4294967296
            """
        );

        SeasonCycleSettings settings = new YamlSeasonCycleSettingsProvider(settingsFile).settings();

        assertEquals(4294967296L, settings.intervalTicks());
        assertEquals(List.of(new SeasonId("custom:dry"), new SeasonId("custom:rainy")),
            settings.calendar().seasons());
        assertEquals(new SeasonId("custom:dry"), settings.calendar().nextAfter(new SeasonId("custom:rainy")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "{}", "null", "[9]", "[spring]", "[custom:dry, custom:dry]"})
    void rejectsInvalidExplicitCalendars(String calendar) throws IOException {
        Path settingsFile = writeFile(
            """
            calendar: %s
            advancement:
              enabled: false
              initial-delay-ticks: 0
              interval-ticks: 1
            """.formatted(calendar)
        );

        assertThrows(IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "-0.5", "18446744073709551617"})
    void rejectsNonintegralOrOutOfRangeTickValues(String ticks) throws IOException {
        Path settingsFile = writeFile(
            """
            advancement:
              enabled: true
              initial-delay-ticks: 0
              interval-ticks: %s
            """.formatted(ticks)
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new YamlSeasonCycleSettingsProvider(settingsFile).settings());
        assertTrue(exception.getMessage().contains("advancement.interval-ticks must be a long integer"));
    }

    private Path writeFile(String content) throws IOException {
        Path settingsFile = temporaryDirectory.resolve("season-cycle.yml");
        Files.writeString(settingsFile, content);
        return settingsFile;
    }
}
