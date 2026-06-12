package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlOreDropPolicyProviderTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsConfiguredMultiplierRangeAsTypedPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        OreDropPolicy policy = new YamlOreDropPolicyProvider(policyFile)
            .policyFor(FOREST);

        assertEquals(FOREST, policy.biomeId());
        assertEquals(
            new OreDropMultiplierRange(1.0, 1.2),
            policy.multiplierRangeFor(IRON_ORE)
        );
    }

    @Test
    void rejectsInvalidMultiplierRangeThroughDomainValidation() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.2
                  max: 1.0
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );
    }

    @Test
    void failsClearlyWhenRequiredPolicyEntryIsMissing() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("max"));
    }

    @Test
    void failsClearlyWhenPolicyKeyIsMissing() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile)
                .policyFor(new BiomeId("minecraft:desert"))
        );

        assertTrue(exception.getMessage().contains("desert"));
    }

    @Test
    void rejectsMalformedBiomePolicyKey() throws IOException {
        Path policyFile = writePolicy(
            """
            forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );
    }

    @Test
    void rejectsMalformedOreKindKey() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );
    }

    @Test
    void rejectsDuplicateBiomePolicyKeys() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.1
                  max: 1.3
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );
    }

    @Test
    void rejectsDuplicateOreKindKeys() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
                minecraft:iron_ore:
                  min: 1.1
                  max: 1.3
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );
    }

    private Path writePolicy(String yaml) throws IOException {
        Path policyFile = temporaryDirectory.resolve("ore-drops.yml");
        Files.writeString(policyFile, yaml);
        return policyFile;
    }
}
