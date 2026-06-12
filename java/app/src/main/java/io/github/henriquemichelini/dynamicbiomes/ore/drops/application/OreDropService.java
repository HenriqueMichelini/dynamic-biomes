package io.github.henriquemichelini.dynamicbiomes.ore.drops.application;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class OreDropService {
    private final OreOriginTrackingService originTracking;
    private final OreDropQuantityCalculator quantityCalculator;

    public OreDropService(
        OreOriginTrackingService originTracking,
        OreDropQuantityCalculator quantityCalculator
    ) {
        this.originTracking = originTracking;
        this.quantityCalculator = quantityCalculator;
    }

    public int calculateDrops(
        BlockPosition position,
        int vanillaFortuneQuantity,
        double selectedMultiplier
    ) {
        if (!originTracking.isEligibleForBiomeMultiplier(position)) {
            return vanillaFortuneQuantity;
        }

        return quantityCalculator.calculate(
            vanillaFortuneQuantity,
            selectedMultiplier
        );
    }
}
