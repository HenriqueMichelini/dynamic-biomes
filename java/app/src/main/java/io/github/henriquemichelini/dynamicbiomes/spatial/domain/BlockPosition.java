package io.github.henriquemichelini.dynamicbiomes.spatial.domain;

import lombok.NonNull;

public record BlockPosition(
    @NonNull WorldReference world,
    int x,
    int y,
    int z
) {
    public BlockPosition {
    }
}
