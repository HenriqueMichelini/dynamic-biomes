package io.github.henriquemichelini.dynamicbiomes.spatial.domain;

import lombok.NonNull;

/** Identifies a 128-by-128 block horizontal region in one world. */
public record EcologicalRegionId(@NonNull WorldReference world, int x, int z) {
    private static final int WIDTH_IN_BLOCKS = 128;

    public EcologicalRegionId {}

    public static EcologicalRegionId containing(@NonNull BlockPosition position) {
        return new EcologicalRegionId(
            position.world(),
            Math.floorDiv(position.x(), WIDTH_IN_BLOCKS),
            Math.floorDiv(position.z(), WIDTH_IN_BLOCKS)
        );
    }
}
