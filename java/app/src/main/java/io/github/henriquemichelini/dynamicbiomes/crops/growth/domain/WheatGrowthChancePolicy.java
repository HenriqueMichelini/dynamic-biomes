package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;

public final class WheatGrowthChancePolicy {
    private final WheatGrowthChanceVariationSource variationSource;
    private final WheatGrowthChance configuredChance;
    private final Map<SeasonId, WheatGrowthSeasonalFactor> seasonalFactors;

    public WheatGrowthChancePolicy(@NonNull WheatGrowthChanceVariationSource variationSource) {
        this(null, Map.of(), variationSource);
    }

    public WheatGrowthChancePolicy(
        WheatGrowthChance configuredChance,
        @NonNull WheatGrowthChanceVariationSource variationSource
    ) {
        this(configuredChance, Map.of(), variationSource);
    }

    public WheatGrowthChancePolicy(
        WheatGrowthChance configuredChance,
        Map<SeasonId, WheatGrowthSeasonalFactor> seasonalFactors,
        @NonNull WheatGrowthChanceVariationSource variationSource
    ) {
        this.variationSource = variationSource;
        this.configuredChance = configuredChance;
        this.seasonalFactors = seasonalFactors == null
            ? Map.of()
            : Map.copyOf(seasonalFactors);
    }

    public WheatGrowthDecision decide() {
        if (configuredChance == null) {
            throw new IllegalStateException(
                "Wheat growth chance policy has no configured chance"
            );
        }
        return decide(configuredChance);
    }

    public WheatGrowthChance configuredChance() {
        if (configuredChance == null) {
            throw new IllegalStateException(
                "Wheat growth chance policy has no configured chance"
            );
        }
        return configuredChance;
    }

    public WheatGrowthChance effectiveChanceFor(@NonNull SeasonId seasonId) {
        WheatGrowthSeasonalFactor seasonalFactor = seasonalFactors.get(seasonId);
        double factor = seasonalFactor == null ? 1.0 : seasonalFactor.factor();
        double effectiveChance = Math.min(1.0, configuredChance().value() * factor);
        return new WheatGrowthChance(effectiveChance);
    }

    public Optional<WheatGrowthSeasonalFactor> seasonalFactorFor(
        @NonNull SeasonId seasonId
    ) {
        return Optional.ofNullable(seasonalFactors.get(seasonId));
    }

    public WheatGrowthDecision decide(@NonNull SeasonId seasonId) {
        return decide(effectiveChanceFor(seasonId));
    }

    public WheatGrowthDecision decide(@NonNull WheatGrowthChance chance) {
        if (chance.value() == 1.0) {
            return WheatGrowthDecision.ALLOW_GROWTH;
        }
        if (chance.value() == 0.0) {
            return WheatGrowthDecision.CANCEL_GROWTH;
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException(
                "Wheat growth chance variation must be within [0.0, 1.0]"
            );
        }

        return variation < chance.value()
            ? WheatGrowthDecision.ALLOW_GROWTH
            : WheatGrowthDecision.CANCEL_GROWTH;
    }
}
