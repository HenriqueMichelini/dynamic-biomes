package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlOreDropPolicyProviderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsConfiguredMultiplierRangeAsTypedPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            forest:
              ores:
                iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        OreDropPolicy policy = new YamlOreDropPolicyProvider(policyFile)
            .policyFor("forest");

        assertEquals(
            new OreDropMultiplierRange(1.0, 1.2),
            policy.multiplierRangeFor("iron_ore")
        );
    }

    @Test
    void rejectsInvalidMultiplierRangeThroughDomainValidation() throws IOException {
        Path policyFile = writePolicy(
            """
            forest:
              ores:
                iron_ore:
                  min: 1.2
                  max: 1.0
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor("forest")
        );
    }

    @Test
    void failsClearlyWhenRequiredPolicyEntryIsMissing() throws IOException {
        Path policyFile = writePolicy(
            """
            forest:
              ores:
                iron_ore:
                  min: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor("forest")
        );

        assertTrue(exception.getMessage().contains("max"));
    }

    @Test
    void failsClearlyWhenPolicyKeyIsMissing() throws IOException {
        Path policyFile = writePolicy(
            """
            forest:
              ores:
                iron_ore:
                  min: 1.0
                  max: 1.2
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor("desert")
        );

        assertTrue(exception.getMessage().contains("desert"));
    }

    private Path writePolicy(String yaml) throws IOException {
        Path policyFile = temporaryDirectory.resolve("ore-drops.yml");
        Files.writeString(policyFile, yaml);
        return policyFile;
    }
}
