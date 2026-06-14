package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;

public final class OreDropOreRule {
    private final OreDropMultiplierRange multiplierRange;
    private final Map<SeasonId, OreDropSeasonalAdjustment> seasonalAdjustments;

    public OreDropOreRule(
        OreDropMultiplierRange multiplierRange,
        Map<SeasonId, OreDropSeasonalAdjustment> seasonalAdjustments
    ) {
        if (multiplierRange == null) {
            throw new IllegalArgumentException(
                "Ore drop rule multiplier range must not be null"
            );
        }
        this.multiplierRange = multiplierRange;
        this.seasonalAdjustments = seasonalAdjustments == null
            ? Map.of()
            : Map.copyOf(seasonalAdjustments);
    }

    public OreDropMultiplierRange multiplierRange() {
        return multiplierRange;
    }

    public double seasonalMultiplierFactorFor(SeasonId seasonId) {
        OreDropSeasonalAdjustment adjustment = seasonalAdjustments.get(seasonId);
        return adjustment == null ? 1.0 : adjustment.multiplierFactor();
    }
}
