package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import java.util.Map;

public final class OreDropPolicy {
    private final BiomeId biomeId;
    private final Map<OreKind, OreDropMultiplierRange> multiplierRangesByOreKind;

    public OreDropPolicy(
        BiomeId biomeId,
        Map<OreKind, OreDropMultiplierRange> multiplierRangesByOreKind
    ) {
        if (biomeId == null) {
            throw new IllegalArgumentException("Ore drop policy biome identity must not be null");
        }
        if (multiplierRangesByOreKind == null || multiplierRangesByOreKind.isEmpty()) {
            throw new IllegalArgumentException("Ore drop policy must define at least one ore multiplier range");
        }

        this.biomeId = biomeId;
        this.multiplierRangesByOreKind = Map.copyOf(multiplierRangesByOreKind);
    }

    public BiomeId biomeId() {
        return biomeId;
    }

    public OreDropMultiplierRange multiplierRangeFor(OreKind oreKind) {
        OreDropMultiplierRange range = multiplierRangesByOreKind.get(oreKind);
        if (range == null) {
            throw new UnsupportedOreDropConfigurationException(
                "Missing ore drop multiplier range for ore kind: " + oreKind
            );
        }
        return range;
    }
}
