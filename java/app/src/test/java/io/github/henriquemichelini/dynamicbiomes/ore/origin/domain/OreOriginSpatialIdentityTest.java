package io.github.henriquemichelini.dynamicbiomes.ore.origin.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OreOriginSpatialIdentityTest {
    @Test
    void worldReferenceRejectsMissingIdentity() {
        assertThrows(NullPointerException.class, () -> new WorldReference(null));
    }

    @Test
    void blockPositionRejectsMissingWorldReference() {
        assertThrows(NullPointerException.class, () -> new BlockPosition(null, 10, 64, -20));
    }

    @Test
    void spatialIdentityHasValueEquality() {
        UUID worldId = UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e");
        BlockPosition position = new BlockPosition(new WorldReference(worldId), 10, 64, -20);
        BlockPosition samePosition = new BlockPosition(new WorldReference(worldId), 10, 64, -20);
        BlockPosition otherWorldPosition = new BlockPosition(
            new WorldReference(UUID.fromString("95c3dd0f-287b-46af-9fd4-00b644dc4a1c")),
            10,
            64,
            -20
        );

        assertAll(
            () -> assertEquals(position, samePosition),
            () -> assertEquals(position.hashCode(), samePosition.hashCode()),
            () -> assertNotEquals(position, otherWorldPosition)
        );
    }
}
