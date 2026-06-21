package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import org.junit.jupiter.api.Test;

class BiomeContextTest {
    private static final BiomeId PLAINS = new BiomeId("minecraft:plains");
    private static final BiomeProfile PLAINS_PROFILE = profileFor(PLAINS);

    @Test
    void containsResolvedIdentityAndMatchingStaticProfile() {
        BiomeContext context = new BiomeContext(PLAINS, PLAINS_PROFILE);

        assertAll(
            () -> assertEquals(PLAINS, context.biomeId()),
            () -> assertEquals(PLAINS_PROFILE, context.profile())
        );
    }

    @Test
    void rejectsMissingIdentityOrProfile() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new BiomeContext(null, PLAINS_PROFILE)),
            () -> assertThrows(NullPointerException.class, () -> new BiomeContext(PLAINS, null))
        );
    }

    @Test
    void rejectsProfileForDifferentBiomeIdentity() {
        BiomeProfile desertProfile = profileFor(new BiomeId("minecraft:desert"));

        assertThrows(
            IllegalArgumentException.class,
            () -> new BiomeContext(PLAINS, desertProfile)
        );
    }

    private static BiomeProfile profileFor(BiomeId biomeId) {
        return new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        );
    }
}
