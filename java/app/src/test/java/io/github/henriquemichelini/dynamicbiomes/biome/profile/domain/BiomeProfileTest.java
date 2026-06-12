package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import org.junit.jupiter.api.Test;

class BiomeProfileTest {
    private static final BiomeId BIOME_ID = new BiomeId("minecraft:plains");
    private static final ClimateProfile CLIMATE = new ClimateProfile(
        new Humidity(0.4),
        new Temperature(0.6)
    );
    private static final Fertility FERTILITY = new Fertility(0.7);
    private static final MineralRichness MINERAL_RICHNESS = new MineralRichness(0.3);
    private static final EcologicalPressure ECOLOGICAL_PRESSURE = new EcologicalPressure(0.2);

    @Test
    void containsOnlyStaticEnvironmentalProperties() {
        BiomeProfile profile = new BiomeProfile(
            BIOME_ID,
            CLIMATE,
            FERTILITY,
            MINERAL_RICHNESS,
            ECOLOGICAL_PRESSURE
        );

        assertAll(
            () -> assertEquals(BIOME_ID, profile.biomeId()),
            () -> assertEquals(CLIMATE, profile.climate()),
            () -> assertEquals(FERTILITY, profile.fertility()),
            () -> assertEquals(MINERAL_RICHNESS, profile.mineralRichness()),
            () -> assertEquals(ECOLOGICAL_PRESSURE, profile.ecologicalPressure())
        );
    }

    @Test
    void rejectsMissingStaticEnvironmentalProperties() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> profile(null, CLIMATE, FERTILITY, MINERAL_RICHNESS, ECOLOGICAL_PRESSURE)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, null, FERTILITY, MINERAL_RICHNESS, ECOLOGICAL_PRESSURE)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, CLIMATE, null, MINERAL_RICHNESS, ECOLOGICAL_PRESSURE)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, CLIMATE, FERTILITY, null, ECOLOGICAL_PRESSURE)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, CLIMATE, FERTILITY, MINERAL_RICHNESS, null))
        );
    }

    private static BiomeProfile profile(
        BiomeId biomeId,
        ClimateProfile climate,
        Fertility fertility,
        MineralRichness mineralRichness,
        EcologicalPressure ecologicalPressure
    ) {
        return new BiomeProfile(
            biomeId,
            climate,
            fertility,
            mineralRichness,
            ecologicalPressure
        );
    }
}
