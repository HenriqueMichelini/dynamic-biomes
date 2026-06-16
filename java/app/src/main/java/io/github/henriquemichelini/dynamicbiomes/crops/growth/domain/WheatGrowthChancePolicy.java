package io.github.henriquemichelini.dynamicbiomes.crops.growth.domain;

import lombok.NonNull;

public final class WheatGrowthChancePolicy {
    private final WheatGrowthChanceVariationSource variationSource;
    private final WheatGrowthChance configuredChance;

    public WheatGrowthChancePolicy(@NonNull WheatGrowthChanceVariationSource variationSource) {
        this(null, variationSource);
    }

    public WheatGrowthChancePolicy(
        WheatGrowthChance configuredChance,
        @NonNull WheatGrowthChanceVariationSource variationSource
    ) {
        this.variationSource = variationSource;
        this.configuredChance = configuredChance;
    }

    public WheatGrowthDecision decide() {
        if (configuredChance == null) {
            throw new IllegalStateException(
                "Wheat growth chance policy has no configured chance"
            );
        }
        return decide(configuredChance);
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
