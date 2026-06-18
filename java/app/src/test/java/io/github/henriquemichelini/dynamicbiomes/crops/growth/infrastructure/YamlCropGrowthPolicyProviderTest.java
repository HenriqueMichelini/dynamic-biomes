package io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlCropGrowthPolicyProviderTest {
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

        CropGrowthDecision decision = new YamlCropGrowthPolicyProvider(
            Path.of(resource.toURI()),
            () -> 0.5
        ).policyFor(FOREST, CropKind.WHEAT).decide();

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
    }

    @Test
    void defaultForestPolicyIncludesConfiguredCarrots()
        throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("crop-growth.yml");
        assertNotNull(resource, "Missing packaged crop-growth.yml");

        CropGrowthPolicy policy = new YamlCropGrowthPolicyProvider(
            Path.of(resource.toURI()),
            () -> 0.5
        ).policyFor(FOREST, CropKind.CARROTS);

        assertEquals(0.65, policy.configuredChance().value());
        assertEquals(0.39, policy.effectiveChanceFor(WINTER).value(), 0.0000001);
    }

    @Test
    void defaultForestPolicyIncludesAllSupportedCropKinds()
        throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("crop-growth.yml");
        assertNotNull(resource, "Missing packaged crop-growth.yml");

        YamlCropGrowthPolicyProvider provider = new YamlCropGrowthPolicyProvider(
            Path.of(resource.toURI()),
            () -> 0.5
        );

        assertAll(
            () -> assertEquals(
                0.75,
                provider.policyFor(FOREST, CropKind.WHEAT).configuredChance().value()
            ),
            () -> assertEquals(
                0.65,
                provider.policyFor(FOREST, CropKind.CARROTS).configuredChance().value()
            ),
            () -> assertEquals(
                0.60,
                provider.policyFor(FOREST, CropKind.POTATOES).configuredChance().value()
            ),
            () -> assertEquals(
                0.55,
                provider.policyFor(FOREST, CropKind.BEETROOT).configuredChance().value()
            )
        );
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

        CropGrowthDecision decision = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> {
                throw new AssertionError("Variation is unnecessary at full chance");
            }
        ).policyFor(FOREST, CropKind.WHEAT).decide();

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, decision);
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

        CropGrowthDecision decision = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> {
                throw new AssertionError("Variation is unnecessary at zero chance");
            }
        ).policyFor(FOREST, CropKind.WHEAT).decide();

        assertEquals(CropGrowthDecision.CANCEL_GROWTH, decision);
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
            CropGrowthDecision.ALLOW_GROWTH,
            new YamlCropGrowthPolicyProvider(policyFile, () -> 0.49)
                .policyFor(FOREST, CropKind.WHEAT)
                .decide()
        );
        assertEquals(
            CropGrowthDecision.CANCEL_GROWTH,
            new YamlCropGrowthPolicyProvider(policyFile, () -> 0.5)
                .policyFor(FOREST, CropKind.WHEAT)
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

        CropGrowthPolicy policy = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> 0.49
        ).policyFor(FOREST, CropKind.WHEAT);

        assertEquals(CropGrowthDecision.ALLOW_GROWTH, policy.decide());
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

        CropGrowthPolicy policy = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> 0.0
        ).policyFor(FOREST, CropKind.WHEAT);

        assertEquals(0.75, policy.effectiveChanceFor(SUMMER).value());
        assertEquals(0.5, policy.effectiveChanceFor(WINTER).value());
    }

    @Test
    void loadsCarrotPolicyWithSeasonalFactors() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                carrots:
                  growth-chance: 0.4
                  seasonal-factors:
                    minecraft:winter: 0.5
            """
        );

        CropGrowthPolicy policy = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> 0.0
        ).policyFor(FOREST, CropKind.CARROTS);

        assertEquals(0.4, policy.configuredChance().value());
        assertEquals(0.2, policy.effectiveChanceFor(WINTER).value());
        assertEquals(0.4, policy.effectiveChanceFor(SUMMER).value());
    }

    @Test
    void loadsRequestedCarrotPolicyWhenAnotherBiomeHasOnlyWheat()
        throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                carrots:
                  growth-chance: 0.4
              minecraft:plains:
                wheat:
                  growth-chance: 0.8
            """
        );

        CropGrowthPolicy policy = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> 0.0
        ).policyFor(FOREST, CropKind.CARROTS);

        assertEquals(0.4, policy.configuredChance().value());
    }

    @Test
    void reportsMissingCarrotPolicyExplicitly() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
            """
        );

        UnsupportedCropGrowthPolicyException exception = assertThrows(
            UnsupportedCropGrowthPolicyException.class,
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.CARROTS)
        );

        assertTrue(exception.getMessage().contains("carrots"));
        assertTrue(exception.getMessage().contains("minecraft:forest"));
    }

    @Test
    void rejectsInvalidCarrotValuesThroughDomainValidation()
        throws IOException {
        Path invalidChancePolicyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                carrots:
                  growth-chance: 1.1
            """
        );
        Path invalidSeasonalFactorPolicyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                carrots:
                  growth-chance: 0.5
                  seasonal-factors:
                    minecraft:winter: -0.1
            """,
            "invalid-carrot-seasonal-factor.yml"
        );

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new YamlCropGrowthPolicyProvider(
                    invalidChancePolicyFile,
                    () -> 0.0
                ).policyFor(FOREST, CropKind.CARROTS)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new YamlCropGrowthPolicyProvider(
                    invalidSeasonalFactorPolicyFile,
                    () -> 0.0
                ).policyFor(FOREST, CropKind.CARROTS)
            )
        );
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

        UnsupportedCropGrowthPolicyException exception = assertThrows(
            UnsupportedCropGrowthPolicyException.class,
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(new BiomeId("minecraft:desert"), CropKind.WHEAT)
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
                () -> new YamlCropGrowthPolicyProvider(aboveMaximumPolicyFile, () -> 0.0)
                    .policyFor(FOREST, CropKind.WHEAT)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new YamlCropGrowthPolicyProvider(belowMinimumPolicyFile, () -> 0.0)
                    .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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

        UnsupportedCropGrowthPolicyException exception = assertThrows(
            UnsupportedCropGrowthPolicyException.class,
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
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
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
        );
    }

    @Test
    void loadsPotatoAndBeetrootPolicies() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                potatoes:
                  growth-chance: 0.6
                beetroot:
                  growth-chance: 0.4
                  seasonal-factors:
                    minecraft:winter: 0.5
            """
        );

        YamlCropGrowthPolicyProvider provider = new YamlCropGrowthPolicyProvider(
            policyFile,
            () -> 0.0
        );

        CropGrowthPolicy potatoPolicy = provider.policyFor(
            FOREST,
            CropKind.POTATOES
        );
        CropGrowthPolicy beetrootPolicy = provider.policyFor(
            FOREST,
            CropKind.BEETROOT
        );

        assertEquals(0.6, potatoPolicy.configuredChance().value());
        assertEquals(0.4, beetrootPolicy.configuredChance().value());
        assertEquals(0.2, beetrootPolicy.effectiveChanceFor(WINTER).value());
    }

    @Test
    void rejectsUnsupportedCropPolicies() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                nether_wart:
                  growth-chance: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
                .policyFor(FOREST, CropKind.WHEAT)
        );

        assertTrue(exception.getMessage().contains("Unsupported crop"));
        assertTrue(exception.getMessage().contains("nether_wart"));
    }

    @Test
    void rejectsUnsupportedCropPoliciesWhenProviderLoads() throws IOException {
        Path policyFile = writePolicy(
            """
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 1.0
                nether_wart:
                  growth-chance: 1.0
            """
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new YamlCropGrowthPolicyProvider(policyFile, () -> 0.0)
        );

        assertTrue(exception.getMessage().contains("Unsupported crop"));
        assertTrue(exception.getMessage().contains("nether_wart"));
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
