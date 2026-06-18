package io.github.henriquemichelini.dynamicbiomes.trees.growth.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TreeGrowthPolicy {
    private final TreeGrowthChance configuredChance;
    private final Map<SeasonId, TreeGrowthSeasonalFactor> seasonalFactors;
    private final TreeGrowthChanceVariationSource variationSource;

    public TreeGrowthPolicy(
        TreeGrowthChance configuredChance,
        TreeGrowthChanceVariationSource variationSource
    ) {
        this(configuredChance, Map.of(), variationSource);
    }

    public TreeGrowthPolicy(
        TreeGrowthChance configuredChance,
        Map<SeasonId, TreeGrowthSeasonalFactor> seasonalFactors,
        TreeGrowthChanceVariationSource variationSource
    ) {
        this.configuredChance = Objects.requireNonNull(configuredChance);
        this.seasonalFactors = seasonalFactors == null
            ? Map.of()
            : Map.copyOf(seasonalFactors);
        this.variationSource = Objects.requireNonNull(variationSource);
    }

    public TreeGrowthDecision decide() {
        return decide(configuredChance);
    }

    public TreeGrowthDecision decide(SeasonId seasonId) {
        return decide(effectiveChanceFor(seasonId));
    }

    public TreeGrowthChance effectiveChanceFor(SeasonId seasonId) {
        Objects.requireNonNull(seasonId);
        TreeGrowthSeasonalFactor seasonalFactor = seasonalFactors.get(seasonId);
        double factor = seasonalFactor == null ? 1.0 : seasonalFactor.factor();
        double effectiveChance = Math.min(1.0, configuredChance.value() * factor);
        return new TreeGrowthChance(effectiveChance);
    }

    public Optional<TreeGrowthSeasonalFactor> seasonalFactorFor(SeasonId seasonId) {
        Objects.requireNonNull(seasonId);
        return Optional.ofNullable(seasonalFactors.get(seasonId));
    }

    private TreeGrowthDecision decide(TreeGrowthChance chance) {
        if (chance.value() == 1.0) {
            return TreeGrowthDecision.ALLOW_GROWTH;
        }
        if (chance.value() == 0.0) {
            return TreeGrowthDecision.CANCEL_GROWTH;
        }

        double variation = variationSource.nextUnitValue();
        if (!Double.isFinite(variation) || variation < 0.0 || variation > 1.0) {
            throw new IllegalArgumentException(
                "Tree growth chance variation must be within [0.0, 1.0]"
            );
        }

        return variation < chance.value()
            ? TreeGrowthDecision.ALLOW_GROWTH
            : TreeGrowthDecision.CANCEL_GROWTH;
    }
}
