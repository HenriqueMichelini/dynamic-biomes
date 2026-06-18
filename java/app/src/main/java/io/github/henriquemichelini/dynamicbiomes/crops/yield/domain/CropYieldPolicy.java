package io.github.henriquemichelini.dynamicbiomes.crops.yield.domain;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;

public final class CropYieldPolicy {
    private final BiomeId biomeId;
    private final Map<CropKind, CropYieldCropRule> cropRules;

    public CropYieldPolicy(
        BiomeId biomeId,
        Map<CropKind, CropYieldCropRule> cropRules
    ) {
        if (biomeId == null) {
            throw new IllegalArgumentException(
                "Crop yield policy biome identity must not be null"
            );
        }
        if (cropRules == null || cropRules.isEmpty()) {
            throw new IllegalArgumentException(
                "Crop yield policy must define at least one crop rule"
            );
        }

        this.biomeId = biomeId;
        this.cropRules = Map.copyOf(cropRules);
    }

    public BiomeId biomeId() {
        return biomeId;
    }

    public CropYieldMultiplierRange multiplierRangeFor(CropKind cropKind) {
        return ruleFor(cropKind).multiplierRange();
    }

    public double seasonalFactorFor(CropKind cropKind, SeasonId seasonId) {
        return ruleFor(cropKind).seasonalFactorFor(seasonId);
    }

    private CropYieldCropRule ruleFor(CropKind cropKind) {
        CropYieldCropRule rule = cropRules.get(cropKind);
        if (rule == null) {
            throw new UnsupportedCropYieldPolicyException(
                "Missing crop yield rule for crop kind: " + cropKind
            );
        }
        return rule;
    }
}
