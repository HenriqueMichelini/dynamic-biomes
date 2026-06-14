package io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.EcologicalPressure;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.MineralRichness;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlBiomeProfileProviderTest {
    private static final BiomeId PLAINS = new BiomeId("minecraft:plains");

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsConfiguredProfileAsTypedDomainProfile() throws IOException {
        Path profileFile = writeProfiles(validProfiles());

        BiomeProfile profile = new YamlBiomeProfileProvider(profileFile)
            .profileFor(PLAINS);

        assertEquals(
            new BiomeProfile(
                PLAINS,
                new ClimateProfile(new Humidity(0.40), new Temperature(0.80)),
                new Fertility(0.70),
                new MineralRichness(0.30),
                new EcologicalPressure(0.20)
            ),
            profile
        );
    }

    @Test
    void returnsNullWhenRequestedProfileIsMissing() throws IOException {
        Path profileFile = writeProfiles(validProfiles());
        BiomeId desert = new BiomeId("minecraft:desert");

        BiomeProfile profile = new YamlBiomeProfileProvider(profileFile).profileFor(desert);

        assertNull(profile);
    }

    @Test
    void failsClearlyWhenConfiguredProfileIsNotAMapping() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:plains:
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(profileFile).profileFor(PLAINS)
        );

        assertTrue(exception.getMessage().contains("must be a mapping"));
    }

    @Test
    void rejectsInvalidEnvironmentalValuesThroughDomainValidation() throws IOException {
        assertInvalidProfileValue("humidity", profileWithClimate("humidity", 1.1));
        assertInvalidProfileValue("temperature", profileWithClimate("temperature", -0.1));
        assertInvalidProfileValue("fertility", profileWithProperty("fertility", 1.1));
        assertInvalidProfileValue(
            "mineral richness",
            profileWithProperty("mineral-richness", -0.1)
        );
        assertInvalidProfileValue(
            "ecological pressure",
            profileWithProperty("ecological-pressure", 1.1)
        );
    }

    @Test
    void rejectsDuplicateBiomeProfileIds() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:plains:
                climate:
                  humidity: 0.40
                  temperature: 0.80
                fertility: 0.70
                mineral-richness: 0.30
                ecological-pressure: 0.20
              minecraft:plains:
                climate:
                  humidity: 0.50
                  temperature: 0.50
                fertility: 0.50
                mineral-richness: 0.50
                ecological-pressure: 0.50
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(profileFile).profileFor(PLAINS)
        );
    }

    @Test
    void failsClearlyWhenRequiredProfileFieldIsMissing() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:plains:
                climate:
                  humidity: 0.40
                  temperature: 0.80
                fertility: 0.70
                ecological-pressure: 0.20
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(profileFile).profileFor(PLAINS)
        );

        assertTrue(exception.getMessage().contains("mineral-richness"));
    }

    @Test
    void failsClearlyWhenProfileYamlIsMalformed() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:plains:
                climate: [
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(profileFile).profileFor(PLAINS)
        );

        assertTrue(exception.getMessage().contains(profileFile.toString()));
    }

    @Test
    void failsClearlyWhenProfileFileIsAbsent() {
        Path missingFile = temporaryDirectory.resolve("missing-biome-profiles.yml");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(missingFile).profileFor(PLAINS)
        );

        assertTrue(exception.getMessage().contains(missingFile.toString()));
    }

    private void assertInvalidProfileValue(String expectedMessage, String yaml)
        throws IOException {
        Path profileFile = writeProfiles(yaml);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlBiomeProfileProvider(profileFile).profileFor(PLAINS)
        );

        assertTrue(exception.getMessage().toLowerCase().contains(expectedMessage));
    }

    private Path writeProfiles(String yaml) throws IOException {
        Path profileFile = temporaryDirectory.resolve("biome-profiles.yml");
        Files.writeString(profileFile, yaml);
        return profileFile;
    }

    private static String validProfiles() {
        return """
            profiles:
              minecraft:plains:
                climate:
                  humidity: 0.40
                  temperature: 0.80
                fertility: 0.70
                mineral-richness: 0.30
                ecological-pressure: 0.20
            """;
    }

    private static String profileWithClimate(String field, double value) {
        double humidity = field.equals("humidity") ? value : 0.40;
        double temperature = field.equals("temperature") ? value : 0.80;
        return """
            profiles:
              minecraft:plains:
                climate:
                  humidity: %s
                  temperature: %s
                fertility: 0.70
                mineral-richness: 0.30
                ecological-pressure: 0.20
            """.formatted(humidity, temperature);
    }

    private static String profileWithProperty(String field, double value) {
        return """
            profiles:
              minecraft:plains:
                climate:
                  humidity: 0.40
                  temperature: 0.80
                fertility: %s
                mineral-richness: %s
                ecological-pressure: %s
            """.formatted(
            field.equals("fertility") ? value : 0.70,
            field.equals("mineral-richness") ? value : 0.30,
            field.equals("ecological-pressure") ? value : 0.20
        );
    }
}
