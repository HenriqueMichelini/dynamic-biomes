package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;

public final class CropGrowthPolicy {
    private final CropGrowthChanceVariationSource variationSource;
    private final CropGrowthChance configuredChance;
    private final Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors;

    public CropGrowthPolicy(@NonNull CropGrowthChanceVariationSource variationSource) {
        this(null, Map.of(), variationSource);
    }

    public CropGrowthPolicy(
        CropGrowthChance configuredChance,
        @NonNull CropGrowthChanceVariationSource variationSource
    ) {
        this(configuredChance, Map.of(), variationSource);
    }

    public CropGrowthPolicy(
        CropGrowthChance configuredChance,
        @NonNull Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors,
        @NonNull CropGrowthChanceVariationSource variationSource
    ) {
        this.variationSource = variationSource;
        this.configuredChance = configuredChance;
        this.seasonalFactors = Map.copyOf(seasonalFactors);
    }

    public CropGrowthDecision decide() {
        if (configuredChance == null) {
            throw new IllegalStateException(
                "Crop growth policy has no configured chance"
            );
        }
        return decide(configuredChance);
    }

    public CropGrowthChance configuredChance() {
        if (configuredChance == null) {
            throw new IllegalStateException(
                "Crop growth policy has no configured chance"
            );
        }
        return configuredChance;
    }

    public CropGrowthChance effectiveChanceFor(@NonNull SeasonId seasonId) {
        CropGrowthSeasonalFactor seasonalFactor = seasonalFactors.get(seasonId);
        double factor = seasonalFactor == null ? 1.0 : seasonalFactor.factor();
        double effectiveChance = Math.min(1.0, configuredChance().value() * factor);
        return new CropGrowthChance(effectiveChance);
    }

    public Optional<CropGrowthSeasonalFactor> seasonalFactorFor(
        @NonNull SeasonId seasonId
    ) {
        return Optional.ofNullable(seasonalFactors.get(seasonId));
    }

    public CropGrowthDecision decide(@NonNull SeasonId seasonId) {
        return decide(effectiveChanceFor(seasonId));
    }

    public CropGrowthDecision decide(@NonNull CropGrowthChance chance) {
        if (chance.value() == 1.0) {
            return CropGrowthDecision.ALLOW_GROWTH;
        }
        if (chance.value() == 0.0) {
            return CropGrowthDecision.CANCEL_GROWTH;
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation >= 1.0) {
            throw new IllegalArgumentException(
                "Crop growth chance variation must be within [0.0, 1.0)"
            );
        }

        return variation < chance.value()
            ? CropGrowthDecision.ALLOW_GROWTH
            : CropGrowthDecision.CANCEL_GROWTH;
    }
}
