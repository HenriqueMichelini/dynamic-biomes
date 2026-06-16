package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlOreDropPolicyProviderTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultForestPolicyConfiguresCommonOverworldOres()
        throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("ore-drops.yml");
        assertNotNull(resource, "Missing packaged ore-drops.yml");

        OreDropPolicy policy = new YamlOreDropPolicyProvider(
            Path.of(resource.toURI())
        ).policyFor(FOREST);

        List.of(
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore"
        ).forEach(oreKey ->
            assertTrue(
                policy.multiplierRangeFor(new OreKind(oreKey)).maximum() <= 1.2,
                () -> "Missing conservative forest ore rule for " + oreKey
            )
        );
    }

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
    void loadsMultiplierRangeUnderMultiplierKey() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  multiplier:
                    min: 1.0
                    max: 1.5
            """
        );

        OreDropPolicy policy = new YamlOreDropPolicyProvider(policyFile)
            .policyFor(FOREST);

        assertEquals(
            new OreDropMultiplierRange(1.0, 1.5),
            policy.multiplierRangeFor(IRON_ORE)
        );
    }

    @Test
    void loadsSeasonalAdjustments() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.5
                  seasonal-adjustments:
                    minecraft:summer:
                      multiplier-factor: 1.10
                    minecraft:winter:
                      multiplier-factor: 0.85
            """
        );

        OreDropPolicy policy = new YamlOreDropPolicyProvider(policyFile)
            .policyFor(FOREST);

        assertEquals(1.10, policy.seasonalMultiplierFactorFor(IRON_ORE, SUMMER));
        assertEquals(0.85, policy.seasonalMultiplierFactorFor(IRON_ORE, WINTER));
    }

    @Test
    void seasonalAdjustmentsAreOptional() throws IOException {
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

        assertEquals(1.0, policy.seasonalMultiplierFactorFor(IRON_ORE, SUMMER));
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
    void rejectsNonPositiveSeasonalMultiplierFactor() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
                  seasonal-adjustments:
                    minecraft:summer:
                      multiplier-factor: 0.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("finite positive"));
    }

    @Test
    void rejectsNonNumericSeasonalMultiplierFactor() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
                  seasonal-adjustments:
                    minecraft:summer:
                      multiplier-factor: "high"
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlOreDropPolicyProvider(policyFile).policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("multiplier-factor"));
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

        UnsupportedOreDropConfigurationException exception = assertThrows(
            UnsupportedOreDropConfigurationException.class,
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
    void rejectsMalformedSeasonIdKey() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
                  seasonal-adjustments:
                    summer:
                      multiplier-factor: 1.10
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

    @Test
    void rejectsDuplicateSeasonalAdjustmentKeys() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              ores:
                minecraft:iron_ore:
                  min: 1.0
                  max: 1.2
                  seasonal-adjustments:
                    minecraft:summer:
                      multiplier-factor: 1.10
                    minecraft:summer:
                      multiplier-factor: 1.20
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
