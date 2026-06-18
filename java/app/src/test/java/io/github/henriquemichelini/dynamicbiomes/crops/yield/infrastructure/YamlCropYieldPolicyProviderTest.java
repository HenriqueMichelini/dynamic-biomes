package io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlCropYieldPolicyProviderTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");

    @TempDir
    private Path tempDirectory;

    @Test
    void loadsDefaultPolicyForAllSupportedCropKinds() throws Exception {
        YamlCropYieldPolicyProvider provider = new YamlCropYieldPolicyProvider(
            defaultResourcePath()
        );

        for (CropKind cropKind : CropKind.values()) {
            assertTrue(
                provider.policyFor(FOREST).multiplierRangeFor(cropKind).minimum() >= 0.0
            );
            assertTrue(
                provider.policyFor(FOREST).seasonalFactorFor(cropKind, SPRING) > 0.0
            );
        }
    }

    @Test
    void loadsConfiguredMultiplierAndSeasonalFactor() throws Exception {
        YamlCropYieldPolicyProvider provider = providerFor("""
            biomes:
              minecraft:forest:
                wheat:
                  multiplier:
                    min: 0.5
                    max: 1.25
                  seasonal-factors:
                    minecraft:spring: 1.4
            """);

        assertEquals(
            0.5,
            provider.policyFor(FOREST).multiplierRangeFor(CropKind.WHEAT).minimum()
        );
        assertEquals(
            1.25,
            provider.policyFor(FOREST).multiplierRangeFor(CropKind.WHEAT).maximum()
        );
        assertEquals(
            1.4,
            provider.policyFor(FOREST).seasonalFactorFor(CropKind.WHEAT, SPRING)
        );
    }

    @Test
    void missingBiomePolicyThrowsUnsupportedPolicy() throws Exception {
        YamlCropYieldPolicyProvider provider = providerFor("""
            biomes:
              minecraft:forest:
                wheat:
                  multiplier:
                    min: 1.0
                    max: 1.0
            """);

        assertThrows(
            UnsupportedCropYieldPolicyException.class,
            () -> provider.policyFor(new BiomeId("minecraft:desert"))
        );
    }

    @Test
    void missingCropRuleThrowsUnsupportedPolicy() throws Exception {
        YamlCropYieldPolicyProvider provider = providerFor("""
            biomes:
              minecraft:forest:
                wheat:
                  multiplier:
                    min: 1.0
                    max: 1.0
            """);

        assertThrows(
            UnsupportedCropYieldPolicyException.class,
            () -> provider.policyFor(FOREST).multiplierRangeFor(CropKind.CARROTS)
        );
    }

    @Test
    void rejectsMalformedYamlAndDuplicateKeys() throws Exception {
        assertThrows(
            IllegalArgumentException.class,
            () -> providerFor("biomes: [")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> providerFor("""
                biomes:
                  minecraft:forest:
                    wheat:
                      multiplier:
                        min: 1.0
                        max: 1.0
                    wheat:
                      multiplier:
                        min: 1.0
                        max: 1.0
                """)
        );
    }

    @Test
    void rejectsInvalidNumbersBiomeIdsAndUnsupportedCropKeys() throws Exception {
        assertThrows(
            IllegalArgumentException.class,
            () -> providerFor("""
                biomes:
                  minecraft:forest:
                    wheat:
                      multiplier:
                        min: -0.1
                        max: 1.0
                """).policyFor(FOREST)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> providerFor("""
                biomes:
                  invalid biome:
                    wheat:
                      multiplier:
                        min: 1.0
                        max: 1.0
                """)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> providerFor("""
                biomes:
                  minecraft:forest:
                    nether_wart:
                      multiplier:
                        min: 1.0
                        max: 1.0
                """)
        );
    }

    private YamlCropYieldPolicyProvider providerFor(String yaml) throws Exception {
        Path policyFile = tempDirectory.resolve("crop-yields.yml");
        Files.writeString(policyFile, yaml);
        return new YamlCropYieldPolicyProvider(policyFile);
    }

    private static Path defaultResourcePath() throws URISyntaxException {
        return Path.of(
            YamlCropYieldPolicyProviderTest.class
                .getClassLoader()
                .getResource("crop-yields.yml")
                .toURI()
        );
    }
}
