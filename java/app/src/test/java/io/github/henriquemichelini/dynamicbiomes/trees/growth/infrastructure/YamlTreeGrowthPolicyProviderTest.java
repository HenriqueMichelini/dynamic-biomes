package io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthDecision;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.UnsupportedTreeGrowthPolicyException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class YamlTreeGrowthPolicyProviderTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final BiomeId PLAINS = new BiomeId("minecraft:plains");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");

    @TempDir
    private Path directory;

    @Test
    void loadsTypedPoliciesAndUsesNeutralFactorForAnUnconfiguredSeason() throws IOException {
        YamlTreeGrowthPolicyProvider provider = providerFor("""
            biomes:
              minecraft:forest:
                growth-chance: 0.8
                seasonal-factors:
                  minecraft:winter: 0.5
              minecraft:plains:
                growth-chance: 1
            """);

        TreeGrowthPolicy forest = provider.policyFor(FOREST);
        assertEquals(new TreeGrowthChance(0.4), forest.effectiveChanceFor(WINTER));
        assertEquals(new TreeGrowthChance(0.8), forest.effectiveChanceFor(SUMMER));
        assertEquals(TreeGrowthDecision.CANCEL_GROWTH, forest.decide(WINTER));
        assertEquals(TreeGrowthDecision.ALLOW_GROWTH, forest.decide(SUMMER));
        assertEquals(new TreeGrowthChance(1.0), provider.policyFor(PLAINS).effectiveChanceFor(WINTER));
    }

    @Test
    void emptyBiomeMappingSupportsNoBiomes() throws IOException {
        YamlTreeGrowthPolicyProvider provider = providerFor("biomes: {}");

        assertThrows(UnsupportedTreeGrowthPolicyException.class, () -> provider.policyFor(FOREST));
    }

    @Test
    void bundledPolicyPreservesForestGrowthInSpringAndChangesItInWinter() throws IOException {
        Path file = directory.resolve("tree-growth.yml");
        try (InputStream bundled = getClass().getResourceAsStream("/tree-growth.yml")) {
            Files.copy(Objects.requireNonNull(bundled), file);
        }
        YamlTreeGrowthPolicyProvider provider = new YamlTreeGrowthPolicyProvider(file, () -> 0.6);

        TreeGrowthPolicy forest = provider.policyFor(FOREST);
        assertEquals(new TreeGrowthChance(1.0), forest.effectiveChanceFor(new SeasonId("minecraft:spring")));
        assertEquals(new TreeGrowthChance(0.4), forest.effectiveChanceFor(WINTER));
        assertThrows(UnsupportedTreeGrowthPolicyException.class, () -> provider.policyFor(PLAINS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[",
        "[]",
        "biomes: []",
        "unknown: {}",
        "biomes: {}\nunknown: {}",
        "biomes: {forest: {growth-chance: 0.8}}",
        "biomes: {minecraft:forest: []}",
        "biomes: {minecraft:forest: {seasonal-factors: {}}}",
        "biomes: {minecraft:forest: {growth-chance: '0.8'}}",
        "biomes: {minecraft:forest: {growth-chance: 1.1}}",
        "biomes: {minecraft:forest: {growth-chance: .nan}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, extra: true}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, seasonal-factors: null}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, seasonal-factors: {winter: 0.5}}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, seasonal-factors: {minecraft:winter: -0.5}}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, seasonal-factors: {minecraft:winter: .inf}}}",
        "biomes: {}\nbiomes: {}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, growth-chance: 0.9}}",
        "biomes: {minecraft:forest: {growth-chance: 0.8, seasonal-factors: {minecraft:winter: 0.5, minecraft:winter: 0.6}}}"
    })
    void rejectsMalformedConfigurationWhenConstructed(String yaml) {
        assertThrows(IllegalArgumentException.class, () -> providerFor(yaml));
    }

    @Test
    void failsWhenPolicyFileCannotBeRead() {
        assertThrows(IllegalArgumentException.class, () -> new YamlTreeGrowthPolicyProvider(
            directory.resolve("missing.yml"),
            () -> 0.6
        ));
    }

    private YamlTreeGrowthPolicyProvider providerFor(String yaml) throws IOException {
        Path file = Files.writeString(directory.resolve("tree-growth.yml"), yaml);
        return new YamlTreeGrowthPolicyProvider(file, () -> 0.6);
    }
}
