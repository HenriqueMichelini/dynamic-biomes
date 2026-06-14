package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;

public final class OreDropPolicy {
    private final BiomeId biomeId;
    private final Map<OreKind, OreDropOreRule> oreRules;

    public OreDropPolicy(
        BiomeId biomeId,
        Map<OreKind, OreDropOreRule> oreRules
    ) {
        if (biomeId == null) {
            throw new IllegalArgumentException(
                "Ore drop policy biome identity must not be null"
            );
        }
        if (oreRules == null || oreRules.isEmpty()) {
            throw new IllegalArgumentException(
                "Ore drop policy must define at least one ore rule"
            );
        }

        this.biomeId = biomeId;
        this.oreRules = Map.copyOf(oreRules);
    }

    public BiomeId biomeId() {
        return biomeId;
    }

    public OreDropMultiplierRange multiplierRangeFor(OreKind oreKind) {
        return ruleFor(oreKind).multiplierRange();
    }

    public double seasonalMultiplierFactorFor(OreKind oreKind, SeasonId seasonId) {
        return ruleFor(oreKind).seasonalMultiplierFactorFor(seasonId);
    }

    private OreDropOreRule ruleFor(OreKind oreKind) {
        OreDropOreRule rule = oreRules.get(oreKind);
        if (rule == null) {
            throw new UnsupportedOreDropConfigurationException(
                "Missing ore drop rule for ore kind: " + oreKind
            );
        }
        return rule;
    }
}
