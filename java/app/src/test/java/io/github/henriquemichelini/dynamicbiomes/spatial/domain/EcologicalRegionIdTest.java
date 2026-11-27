package io.github.henriquemichelini.dynamicbiomes.spatial.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EcologicalRegionIdTest {
    private static final WorldReference WORLD = new WorldReference(
        UUID.fromString("00000000-0000-0000-0000-000000000001")
    );

    @Test
    void groupsPositionsByHorizontalRegionAndWorld() {
        EcologicalRegionId region = EcologicalRegionId.containing(
            new BlockPosition(WORLD, 0, -64, 0)
        );

        assertEquals(new EcologicalRegionId(WORLD, 0, 0), region);
        assertEquals(
            region,
            EcologicalRegionId.containing(new BlockPosition(WORLD, 127, 319, 127))
        );
        assertNotEquals(
            region,
            EcologicalRegionId.containing(
                new BlockPosition(new WorldReference(new UUID(0, 2)), 0, -64, 0)
            )
        );
    }

    @Test
    void usesFloorDivisionAtNegativeAndPositiveRegionBoundaries() {
        assertEquals(
            new EcologicalRegionId(WORLD, -1, 1),
            EcologicalRegionId.containing(new BlockPosition(WORLD, -128, 0, 128))
        );
        assertEquals(
            new EcologicalRegionId(WORLD, -2, -1),
            EcologicalRegionId.containing(new BlockPosition(WORLD, -129, 0, -1))
        );
    }
}
