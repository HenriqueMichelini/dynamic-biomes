package io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BiomeResolverTest {
    @Test
    void resolvesPureBiomeContextFromBlockPosition() {
        BlockPosition position = new BlockPosition(
            new WorldReference(UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")),
            10,
            64,
            -20
        );
        BiomeId biomeId = new BiomeId("minecraft:plains");
        BiomeContext expected = new BiomeContext(biomeId, profileFor(biomeId));
        BiomeResolver resolver = requestedPosition -> {
            assertEquals(position, requestedPosition);
            return expected;
        };

        assertEquals(expected, resolver.resolve(position));
    }

    private static BiomeProfile profileFor(BiomeId biomeId) {
        return new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        );
    }
}
