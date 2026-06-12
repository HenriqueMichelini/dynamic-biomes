package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreOriginTest {
    private static final WorldReference WORLD = new WorldReference(
        UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e")
    );
    private static final BlockPosition POSITION = new BlockPosition(WORLD, 10, 64, -20);

    @Test
    void naturalOreIsEligibleForBiomeBasedMultipliers() {
        OreOrigin origin = new OreOrigin(POSITION, OreOriginType.NATURAL);

        assertTrue(origin.isEligibleForBiomeBasedMultiplier());
    }

    @Test
    void playerPlacedOreIsNotEligibleForBiomeBasedMultipliers() {
        OreOrigin origin = new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED);

        assertFalse(origin.isEligibleForBiomeBasedMultiplier());
    }

    @Test
    void rejectsMissingRequiredLocationOrOriginType() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> new OreOrigin(null, OreOriginType.NATURAL)),
            () -> assertThrows(NullPointerException.class, () -> new OreOrigin(POSITION, null))
        );
    }

    @Test
    void equalityIsByLocationAndOriginTypeValue() {
        OreOrigin naturalOrigin = new OreOrigin(POSITION, OreOriginType.NATURAL);
        OreOrigin sameNaturalOrigin = new OreOrigin(
            new BlockPosition(new WorldReference(WORLD.id()), 10, 64, -20),
            OreOriginType.NATURAL
        );
        OreOrigin differentLocation = new OreOrigin(
            new BlockPosition(WORLD, 11, 64, -20),
            OreOriginType.NATURAL
        );
        OreOrigin differentType = new OreOrigin(POSITION, OreOriginType.PLAYER_PLACED);

        assertAll(
            () -> assertEquals(naturalOrigin, sameNaturalOrigin),
            () -> assertEquals(naturalOrigin.hashCode(), sameNaturalOrigin.hashCode()),
            () -> assertNotEquals(naturalOrigin, differentLocation),
            () -> assertNotEquals(naturalOrigin, differentType)
        );
    }
}
