package io.github.henriquemichelini.dynamicbiomes.crops.performance.application;

import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.OptionalDouble;
import lombok.NonNull;

public final class CropPerformanceService {

    private static final CropPerformanceResult NEUTRAL_PERFORMANCE =
        new CropPerformanceResult(OptionalDouble.empty(), 1.0, 1.0, 1.0);

    private final EnvironmentalStateComposer environmentalStateComposer;
    private final CropPerformanceProfileProvider profileProvider;
    private final CropPerformanceCalculator calculator;

    public CropPerformanceService(
        @NonNull EnvironmentalStateComposer environmentalStateComposer,
        @NonNull CropPerformanceProfileProvider profileProvider,
        @NonNull CropPerformanceCalculator calculator
    ) {
        this.environmentalStateComposer = environmentalStateComposer;
        this.profileProvider = profileProvider;
        this.calculator = calculator;
    }

    public CropPerformanceResult performanceFor(
        @NonNull BlockPosition position,
        @NonNull CropKind cropKind
    ) {
        CropEnvironmentalState environmentalState =
            environmentalStateComposer.compose(position);

        try {
            CropPerformanceProfile profile = profileProvider.profileFor(cropKind);
            return calculator.calculate(profile, environmentalState);
        } catch (UnsupportedCropPerformanceProfileException exception) {
            return NEUTRAL_PERFORMANCE;
        }
    }
}
