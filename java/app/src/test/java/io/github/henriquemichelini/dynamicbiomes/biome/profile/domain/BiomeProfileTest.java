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

    @Test
    void containsOnlyStaticEnvironmentalProperties() {
        BiomeProfile profile = new BiomeProfile(
            BIOME_ID,
            CLIMATE,
            FERTILITY
        );

        assertAll(
            () -> assertEquals(BIOME_ID, profile.biomeId()),
            () -> assertEquals(CLIMATE, profile.climate()),
            () -> assertEquals(FERTILITY, profile.fertility())
        );
    }

    @Test
    void rejectsMissingStaticEnvironmentalProperties() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> profile(null, CLIMATE, FERTILITY)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, null, FERTILITY)),
            () -> assertThrows(NullPointerException.class, () -> profile(BIOME_ID, CLIMATE, null))
        );
    }

    private static BiomeProfile profile(
        BiomeId biomeId,
        ClimateProfile climate,
        Fertility fertility
    ) {
        return new BiomeProfile(
            biomeId,
            climate,
            fertility
        );
    }
}
