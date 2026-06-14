package io.github.henriquemichelini.dynamicbiomes.ore.drops.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OreDropPolicyTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void returnsMultiplierRangeForTypedOreKind() {
        OreDropMultiplierRange range = new OreDropMultiplierRange(1.0, 1.2);
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(IRON_ORE, new OreDropOreRule(range, Map.of()))
        );

        assertEquals(FOREST, policy.biomeId());
        assertEquals(range, policy.multiplierRangeFor(IRON_ORE));
    }

    @Test
    void returnsConfiguredSeasonalMultiplierFactor() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(
                IRON_ORE,
                new OreDropOreRule(
                    new OreDropMultiplierRange(1.0, 1.2),
                    Map.of(SUMMER, new OreDropSeasonalAdjustment(1.10))
                )
            )
        );

        assertEquals(1.10, policy.seasonalMultiplierFactorFor(IRON_ORE, SUMMER));
    }

    @Test
    void returnsNeutralFactorWhenSeasonHasNoAdjustment() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(
                IRON_ORE,
                new OreDropOreRule(
                    new OreDropMultiplierRange(1.0, 1.2),
                    Map.of(SUMMER, new OreDropSeasonalAdjustment(1.10))
                )
            )
        );

        assertEquals(1.0, policy.seasonalMultiplierFactorFor(IRON_ORE, WINTER));
    }

    @Test
    void failsClearlyWhenOreKindIsMissing() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(IRON_ORE, new OreDropOreRule(
                new OreDropMultiplierRange(1.0, 1.2),
                Map.of()
            ))
        );
        OreKind missingOre = new OreKind("minecraft:gold_ore");

        assertThrows(
            UnsupportedOreDropConfigurationException.class,
            () -> policy.multiplierRangeFor(missingOre)
        );
    }

    @Test
    void failsClearlyWhenSeasonalFactorIsRequestedForMissingOreKind() {
        OreDropPolicy policy = new OreDropPolicy(
            FOREST,
            Map.of(IRON_ORE, new OreDropOreRule(
                new OreDropMultiplierRange(1.0, 1.2),
                Map.of()
            ))
        );
        OreKind missingOre = new OreKind("minecraft:gold_ore");

        assertThrows(
            UnsupportedOreDropConfigurationException.class,
            () -> policy.seasonalMultiplierFactorFor(missingOre, SUMMER)
        );
    }
}
