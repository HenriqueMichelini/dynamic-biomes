package io.github.henriquemichelini.dynamicbiomes.crops.performance.application;

import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public interface EnvironmentalStateComposer {
    CropEnvironmentalState compose(BlockPosition position);
}
