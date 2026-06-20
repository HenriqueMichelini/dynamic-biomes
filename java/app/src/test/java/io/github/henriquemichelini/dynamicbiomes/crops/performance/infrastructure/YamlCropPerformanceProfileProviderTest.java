package io.github.henriquemichelini.dynamicbiomes.crops.performance.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlCropPerformanceProfileProviderTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void defaultResourceLoadsAllSupportedCropProfiles()
        throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("crop-profiles.yml");
        assertNotNull(resource, "Missing packaged crop-profiles.yml");

        YamlCropPerformanceProfileProvider provider =
            new YamlCropPerformanceProfileProvider(Path.of(resource.toURI()));

        assertAll(
            () -> assertEquals(
                CropKind.WHEAT,
                provider.profileFor(CropKind.WHEAT).cropKind()
            ),
            () -> assertEquals(
                CropKind.CARROTS,
                provider.profileFor(CropKind.CARROTS).cropKind()
            ),
            () -> assertEquals(
                CropKind.POTATOES,
                provider.profileFor(CropKind.POTATOES).cropKind()
            ),
            () -> assertEquals(
                CropKind.BEETROOT,
                provider.profileFor(CropKind.BEETROOT).cropKind()
            )
        );
    }

    @Test
    void loadsConfiguredProfileAsTypedDomainProfile() throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              wheat:
                preferences:
                  wind-speed: 0.10
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
            """);

        CropPerformanceProfile profile = new YamlCropPerformanceProfileProvider(
            profileFile
        ).profileFor(CropKind.WHEAT);

        assertAll(
            () -> assertEquals(CropKind.WHEAT, profile.cropKind()),
            () -> assertEquals(0.10, profile.preferredWindSpeed().normalized()),
            () -> assertEquals(0.20, profile.preferredRainStrength().normalized()),
            () -> assertEquals(0.30, profile.preferredHumidity().normalized()),
            () -> assertEquals(0.40, profile.preferredTemperature().normalized()),
            () -> assertEquals(0.50, profile.preferredSolarIncidence().normalized()),
            () -> assertEquals(0.60, profile.preferredSoilFertility().normalized())
        );
    }

    @Test
    void missingCropProfileThrowsUnsupportedProfileException()
        throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              wheat:
                preferences:
                  wind-speed: 0.10
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
            """);

        UnsupportedCropPerformanceProfileException exception = assertThrows(
            UnsupportedCropPerformanceProfileException.class,
            () -> new YamlCropPerformanceProfileProvider(profileFile)
                .profileFor(CropKind.CARROTS)
        );

        assertTrue(exception.getMessage().contains("carrots"));
    }

    @Test
    void rejectsUnsupportedCropProfileKeysWhenProviderLoads()
        throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              nether_wart:
                preferences:
                  wind-speed: 0.10
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
            """);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropPerformanceProfileProvider(profileFile)
        );

        assertTrue(exception.getMessage().contains("Unsupported crop performance profile"));
        assertTrue(exception.getMessage().contains("nether_wart"));
    }

    @Test
    void rejectsDuplicateCropProfileKeys()
        throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              wheat:
                preferences:
                  wind-speed: 0.10
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
              wheat:
                preferences:
                  wind-speed: 0.20
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
            """);

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropPerformanceProfileProvider(profileFile)
        );
    }

    @Test
    void rejectsInvalidNormalizedPreferenceValuesThroughDomainValidation()
        throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              wheat:
                preferences:
                  wind-speed: 1.10
                  rain-strength: 0.20
                  humidity: 0.30
                  temperature: 0.40
                  solar-incidence: 0.50
                  soil-fertility: 0.60
            """);

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropPerformanceProfileProvider(profileFile)
        );
    }

    @Test
    void rejectsMalformedYaml()
        throws IOException {
        Path profileFile = writeProfiles("""
            profiles:
              wheat:
                preferences: [
            """);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropPerformanceProfileProvider(profileFile)
        );

        assertTrue(exception.getMessage().contains(profileFile.toString()));
    }

    @Test
    void reportsIoFailureForMissingFile() {
        Path missingFile = tempDirectory.resolve("missing-crop-profiles.yml");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropPerformanceProfileProvider(missingFile)
        );

        assertTrue(exception.getMessage().contains(missingFile.toString()));
    }

    private Path writeProfiles(String yaml) throws IOException {
        Path profileFile = tempDirectory.resolve("crop-profiles.yml");
        Files.writeString(profileFile, yaml);
        return profileFile;
    }
}
