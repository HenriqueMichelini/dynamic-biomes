package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedWheatGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlWheatGrowthChancePolicyProviderTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @TempDir
    Path temporaryDirectory;

    @Test
    void defaultForestPolicyAllowsDeterministicWheatGrowth()
        throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("crop-growth.yml");
        assertNotNull(resource, "Missing packaged crop-growth.yml");

        WheatGrowthDecision decision = new YamlWheatGrowthChancePolicyProvider(
            Path.of(resource.toURI()),
            () -> 0.5
        ).policyFor(FOREST).decide();

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void loadsFullChanceWheatPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
            """
        );

        WheatGrowthDecision decision = new YamlWheatGrowthChancePolicyProvider(
            policyFile,
            () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }
        ).policyFor(FOREST).decide();

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void loadsZeroChanceWheatPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.0
            """
        );

        WheatGrowthDecision decision = new YamlWheatGrowthChancePolicyProvider(
            policyFile,
            () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            }
        ).policyFor(FOREST).decide();

        assertEquals(WheatGrowthDecision.CANCEL_GROWTH, decision);
    }

    @Test
    void delegatesFractionalChanceDecisionToVariationSource() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.5
            """
        );

        assertEquals(
            WheatGrowthDecision.ALLOW_GROWTH,
            new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.49)
                .policyFor(FOREST)
                .decide()
        );
        assertEquals(
            WheatGrowthDecision.CANCEL_GROWTH,
            new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.5)
                .policyFor(FOREST)
                .decide()
        );
    }

    @Test
    void noSeasonalFactorsKeepsNoSeasonBehaviorAndBaseEffectiveChance()
        throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.5
            """
        );

        WheatGrowthChancePolicy policy = new YamlWheatGrowthChancePolicyProvider(
            policyFile,
            () -> 0.49
        ).policyFor(FOREST);

        assertEquals(WheatGrowthDecision.ALLOW_GROWTH, policy.decide());
        assertEquals(0.5, policy.effectiveChanceFor(SUMMER).value());
    }

    @Test
    void loadsSeasonalFactorsForWheatPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.5
                  seasonal-factors:
                    minecraft:summer: 1.5
            """
        );

        WheatGrowthChancePolicy policy = new YamlWheatGrowthChancePolicyProvider(
            policyFile,
            () -> 0.0
        ).policyFor(FOREST);

        assertEquals(0.75, policy.effectiveChanceFor(SUMMER).value());
        assertEquals(0.5, policy.effectiveChanceFor(WINTER).value());
    }

    @Test
    void reportsUnsupportedBiomePolicyExplicitly() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
            """
        );

        UnsupportedWheatGrowthPolicyException exception = assertThrows(
            UnsupportedWheatGrowthPolicyException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(new BiomeId("minecraft:desert"))
        );

        assertTrue(exception.getMessage().contains("minecraft:desert"));
    }

    @Test
    void rejectsInvalidChanceThroughDomainValidation() throws IOException {
        Path aboveMaximumPolicyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.1
            """
        );
        Path belowMinimumPolicyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: -0.1
            """,
            "below-minimum-crop-growth.yml"
        );

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new YamlWheatGrowthChancePolicyProvider(aboveMaximumPolicyFile, () -> 0.0)
                    .policyFor(FOREST)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new YamlWheatGrowthChancePolicyProvider(belowMinimumPolicyFile, () -> 0.0)
                    .policyFor(FOREST)
            )
        );
    }

    @Test
    void rejectsNegativeSeasonalFactorThroughDomainValidation()
        throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.5
                  seasonal-factors:
                    minecraft:winter: -0.1
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );
    }

    @Test
    void rejectsDuplicateSeasonalFactorKeys() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.5
                  seasonal-factors:
                    minecraft:winter: 0.5
                    minecraft:winter: 0.75
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );
    }

    @Test
    void rejectsMissingBiomesRoot() throws IOException {
        Path policyFile = writePolicy(
            """
            minecraft:forest:
              wheat:
                growth-chance: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("biomes"));
    }

    @Test
    void rejectsMissingWheatPolicy() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest: {}
            """
        );

        UnsupportedWheatGrowthPolicyException exception = assertThrows(
            UnsupportedWheatGrowthPolicyException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("minecraft:forest"));
    }

    @Test
    void rejectsMissingGrowthChance() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat: {}
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("growth-chance"));
    }

    @Test
    void rejectsNonNumericGrowthChance() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: "fast"
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("growth-chance"));
    }

    @Test
    void rejectsMalformedBiomePolicyKey() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              forest:
                wheat:
                  growth-chance: 1.0
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );
    }

    @Test
    void rejectsDuplicateBiomePolicyKeys() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
              minecraft:forest:
                wheat:
                  growth-chance: 0.0
            """
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );
    }

    @Test
    void rejectsNonWheatCropPolicies() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                carrots:
                  growth-chance: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlWheatGrowthChancePolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST)
        );

        assertTrue(exception.getMessage().contains("Unsupported crop"));
    }

    private Path writePolicy(String yaml) throws IOException {
        return writePolicy(yaml, "crop-growth.yml");
    }

    private Path writePolicy(String yaml, String fileName) throws IOException {
        Path policyFile = temporaryDirectory.resolve(fileName);
        Files.writeString(policyFile, yaml);
        return policyFile;
    }
}
