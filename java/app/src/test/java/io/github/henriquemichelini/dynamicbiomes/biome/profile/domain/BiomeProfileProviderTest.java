package io.github.henriquemichelini.dynamicbiomes.biome.profile.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import org.junit.jupiter.api.Test;

class BiomeProfileProviderTest {
    @Test
    void looksUpTypedProfileByBiomeIdentity() {
        BiomeId biomeId = new BiomeId("minecraft:plains");
        BiomeProfile expected = new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        );
        BiomeProfileProvider provider = requestedBiomeId -> {
            assertEquals(biomeId, requestedBiomeId);
            return expected;
        };

        assertEquals(expected, provider.profileFor(biomeId));
    }
}
