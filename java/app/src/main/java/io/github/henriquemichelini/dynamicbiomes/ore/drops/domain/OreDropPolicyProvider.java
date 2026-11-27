package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;

public interface OreDropPolicyProvider {
    /** @throws UnsupportedOreDropConfigurationException if the biome has no ore drop policy */
    OreDropPolicy policyFor(BiomeId biomeId);
}
