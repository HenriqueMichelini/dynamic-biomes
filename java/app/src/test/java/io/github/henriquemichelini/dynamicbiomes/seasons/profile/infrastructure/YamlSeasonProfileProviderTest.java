package io.github.henriquemichelini.dynamicbiomes.seasons.profile.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlSeasonProfileProviderTest {
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsConfiguredProfileAsTypedDomainProfile() throws IOException {
        Path profileFile = writeProfiles(validProfiles());

        SeasonProfile profile = new YamlSeasonProfileProvider(profileFile).profileFor(SPRING);

        assertEquals(
            new SeasonProfile(
                SPRING,
                new SeasonClimateAdjustment(
                    new SeasonalAdjustment(0.10),
                    new SeasonalAdjustment(0.20)
                )
            ),
            profile
        );
    }

    @Test
    void failsClearlyWhenRequestedProfileIsMissing() throws IOException {
        Path profileFile = writeProfiles(validProfiles());
        SeasonId winter = new SeasonId("minecraft:winter");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(winter)
        );

        assertTrue(exception.getMessage().contains(winter.value()));
    }

    @Test
    void rejectsInvalidAdjustmentValuesThroughDomainValidation() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:spring:
                climate-adjustment:
                  temperature: 1.1
                  humidity: 0.20
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(SPRING)
        );

        assertTrue(exception.getMessage().contains("Seasonal adjustment"));
    }

    @Test
    void rejectsDuplicateSeasonProfileIds() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:spring:
                climate-adjustment:
                  temperature: 0.10
                  humidity: 0.20
              minecraft:spring:
                climate-adjustment:
                  temperature: 0.30
                  humidity: 0.40
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(SPRING)
        );
    }

    @Test
    void rejectsInvalidSeasonProfileIds() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              spring:
                climate-adjustment:
                  temperature: 0.10
                  humidity: 0.20
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(SPRING)
        );

        assertTrue(exception.getMessage().contains("Season identity"));
    }

    @Test
    void failsClearlyWhenRequiredProfileFieldIsMissing() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:spring:
                climate-adjustment:
                  temperature: 0.10
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(SPRING)
        );

        assertTrue(exception.getMessage().contains("humidity"));
    }

    @Test
    void failsClearlyWhenProfileYamlIsMalformed() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:spring:
                climate-adjustment: [
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(profileFile).profileFor(SPRING)
        );

        assertTrue(exception.getMessage().contains(profileFile.toString()));
    }

    @Test
    void failsClearlyWhenProfileFileIsAbsent() {
        Path missingFile = temporaryDirectory.resolve("missing-season-profiles.yml");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlSeasonProfileProvider(missingFile).profileFor(SPRING)
        );

        assertTrue(exception.getMessage().contains(missingFile.toString()));
    }

    private Path writeProfiles(String yaml) throws IOException {
        Path profileFile = temporaryDirectory.resolve("season-profiles.yml");
        Files.writeString(profileFile, yaml);
        return profileFile;
    }

    private static String validProfiles() {
        return """
            profiles:
              minecraft:spring:
                climate-adjustment:
                  temperature: 0.10
                  humidity: 0.20
            """;
    }
}
