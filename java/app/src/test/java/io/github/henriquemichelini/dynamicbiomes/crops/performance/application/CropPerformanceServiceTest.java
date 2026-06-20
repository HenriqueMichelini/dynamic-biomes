package io.github.henriquemichelini.dynamicbiomes.crops.performance.application;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CropPerformanceServiceTest {

    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(UUID.fromString("35e4a921-a622-4e10-a336-095d5a857f20")),
        12,
        64,
        -8
    );

    @Test
    void returnsDomainCalculatedPerformanceForSupportedCropProfile() {
        CropEnvironmentalState state = state(0.50, 0.40, 0.80, 0.60, 0.70, 0.90);
        CropPerformanceProfile profile = profile(0.50, 0.40, 0.80, 0.60, 0.70, 0.90);
        RecordingComposer composer = new RecordingComposer(state);
        RecordingProfileProvider profileProvider = new RecordingProfileProvider(profile);

        CropPerformanceResult result = serviceWith(composer, profileProvider)
            .performanceFor(POSITION, CropKind.WHEAT);

        assertAll(
            () -> assertEquals(1.0, result.overallScore().orElseThrow()),
            () -> assertEquals(1.0, result.growthSpeedFactor()),
            () -> assertEquals(1.0, result.growthChanceFactor()),
            () -> assertEquals(1.0, result.harvestQuantityFactor()),
            () -> assertEquals(POSITION, composer.requestedPosition),
            () -> assertEquals(CropKind.WHEAT, profileProvider.requestedCropKind)
        );
    }

    @Test
    void returnsNeutralPerformanceForMissingOrUnsupportedCropProfile() {
        CropPerformanceService service = serviceWith(
            new RecordingComposer(state(0.50, 0.40, 0.80, 0.60, 0.70, 0.90)),
            cropKind -> {
                throw new UnsupportedCropPerformanceProfileException("missing profile");
            }
        );

        CropPerformanceResult result = service.performanceFor(POSITION, CropKind.CARROTS);

        assertAll(
            () -> assertEquals(OptionalDouble.empty(), result.overallScore()),
            () -> assertEquals(1.0, result.growthSpeedFactor()),
            () -> assertEquals(1.0, result.growthChanceFactor()),
            () -> assertEquals(1.0, result.harvestQuantityFactor())
        );
    }

    @Test
    void propagatesUnsupportedBiomeFromEnvironmentalComposition() {
        CropPerformanceService service = serviceWith(
            position -> {
                throw new UnsupportedBiomeException("unsupported biome");
            },
            cropKind -> {
                throw new AssertionError("Profile lookup should be skipped");
            }
        );

        assertThrows(
            UnsupportedBiomeException.class,
            () -> service.performanceFor(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void propagatesRealFailuresFromEnvironmentalComposition() {
        CropPerformanceService service = serviceWith(
            position -> {
                throw new IllegalStateException("composition failed");
            },
            cropKind -> {
                throw new AssertionError("Profile lookup should be skipped");
            }
        );

        assertThrows(
            IllegalStateException.class,
            () -> service.performanceFor(POSITION, CropKind.WHEAT)
        );
    }

    @Test
    void propagatesRealFailuresFromProfileProvider() {
        CropPerformanceService service = serviceWith(
            new RecordingComposer(state(0.50, 0.40, 0.80, 0.60, 0.70, 0.90)),
            cropKind -> {
                throw new IllegalArgumentException("invalid profile configuration");
            }
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.performanceFor(POSITION, CropKind.WHEAT)
        );
    }

    private static CropPerformanceService serviceWith(
        EnvironmentalStateComposer composer,
        CropPerformanceProfileProvider profileProvider
    ) {
        return new CropPerformanceService(
            composer,
            profileProvider,
            new CropPerformanceCalculator()
        );
    }

    private static CropPerformanceProfile profile(
        double windSpeed,
        double rainStrength,
        double humidity,
        double temperature,
        double solarIncidence,
        double soilFertility
    ) {
        return new CropPerformanceProfile(
            CropKind.WHEAT,
            value(windSpeed),
            value(rainStrength),
            value(humidity),
            value(temperature),
            value(solarIncidence),
            value(soilFertility)
        );
    }

    private static CropEnvironmentalState state(
        double windSpeed,
        double rainStrength,
        double humidity,
        double temperature,
        double solarIncidence,
        double soilFertility
    ) {
        return new CropEnvironmentalState(
            value(windSpeed),
            value(rainStrength),
            value(humidity),
            value(temperature),
            value(solarIncidence),
            value(soilFertility)
        );
    }

    private static NormalizedEnvironmentalValue value(double normalized) {
        return new NormalizedEnvironmentalValue(normalized);
    }

    private static final class RecordingComposer implements EnvironmentalStateComposer {
        private final CropEnvironmentalState state;
        private BlockPosition requestedPosition;

        private RecordingComposer(CropEnvironmentalState state) {
            this.state = state;
        }

        @Override
        public CropEnvironmentalState compose(BlockPosition position) {
            requestedPosition = position;
            return state;
        }
    }

    private static final class RecordingProfileProvider
        implements CropPerformanceProfileProvider {

        private final CropPerformanceProfile profile;
        private CropKind requestedCropKind;

        private RecordingProfileProvider(CropPerformanceProfile profile) {
            this.profile = profile;
        }

        @Override
        public CropPerformanceProfile profileFor(CropKind cropKind) {
            requestedCropKind = cropKind;
            return profile;
        }
    }
}
