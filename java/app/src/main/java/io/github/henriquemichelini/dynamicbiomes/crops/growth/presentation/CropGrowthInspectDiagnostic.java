package io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.EnvironmentalStateComposer;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceResult;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.UnsupportedCropPerformanceProfileException;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Optional;
import java.util.OptionalDouble;
import lombok.NonNull;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

public final class CropGrowthInspectDiagnostic {

    private static final CropPerformanceResult NEUTRAL_PERFORMANCE =
        new CropPerformanceResult(OptionalDouble.empty(), 1.0, 1.0, 1.0);

    private final BiomeResolver biomeResolver;
    private final CropGrowthPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final SeasonProfileProvider seasonProfileProvider;
    private final EnvironmentalStateComposer environmentalStateComposer;
    private final CropPerformanceProfileProvider cropPerformanceProfileProvider;
    private final CropPerformanceCalculator cropPerformanceCalculator;
    private final CropYieldPolicyProvider cropYieldPolicyProvider;

    public CropGrowthInspectDiagnostic(
        @NonNull BiomeResolver biomeResolver,
        @NonNull CropGrowthPolicyProvider policyProvider,
        @NonNull CurrentSeasonQuery currentSeasonQuery,
        @NonNull SeasonProfileProvider seasonProfileProvider,
        @NonNull EnvironmentalStateComposer environmentalStateComposer,
        @NonNull CropPerformanceProfileProvider cropPerformanceProfileProvider,
        @NonNull CropPerformanceCalculator cropPerformanceCalculator,
        @NonNull CropYieldPolicyProvider cropYieldPolicyProvider
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.seasonProfileProvider = seasonProfileProvider;
        this.environmentalStateComposer = environmentalStateComposer;
        this.cropPerformanceProfileProvider = cropPerformanceProfileProvider;
        this.cropPerformanceCalculator = cropPerformanceCalculator;
        this.cropYieldPolicyProvider = cropYieldPolicyProvider;
    }

    public boolean inspect(CommandSender sender, Block targetBlock) {
        CropKind cropKind = PaperCropMaterialMapper.cropKindFor(
            targetBlock.getType()
        ).orElse(null);

        if (cropKind == null) {
            return false;
        }

        BlockPosition position = positionOf(targetBlock);
        BiomeContext biomeContext;

        try {
            biomeContext = biomeResolver.resolve(position);
        } catch (UnsupportedBiomeException exception) {
            String biomeId = exception
                .biomeId()
                .map(BiomeId::value)
                .orElse("unknown");
            sender.sendMessage("Current biome: " + biomeId);
            sender.sendMessage("DynamicBiomes profile: unsupported");
            sender.sendMessage(
                displayName(cropKind) + " growth policy: unsupported"
            );
            sender.sendMessage(
                "May cancel natural growth: no (vanilla fallback)"
            );
            return true;
        }

        sender.sendMessage("Current biome: " + biomeContext.biomeId().value());
        sender.sendMessage("DynamicBiomes profile: supported");

        try {
            CropGrowthPolicy policy = policyProvider.policyFor(
                biomeContext.biomeId(),
                cropKind
            );
            CropGrowthChance configuredChance = policy.configuredChance();
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            Optional<CropGrowthSeasonalFactor> seasonalFactor =
                policy.seasonalFactorFor(currentSeason);
            CropGrowthChance effectiveChance = policy.effectiveChanceFor(
                currentSeason
            );
            SeasonProfile seasonProfile = seasonProfileProvider.profileFor(
                currentSeason
            );
            SeasonClimateAdjustment seasonClimate =
                seasonProfile.climateAdjustment();
            String displayName = displayName(cropKind);
            String lowerDisplayName = lowerDisplayName(cropKind);
            sender.sendMessage(displayName + " growth policy: supported");
            sender.sendMessage(
                "Configured " +
                    lowerDisplayName +
                    " growth chance: " +
                    configuredChance.value()
            );
            sender.sendMessage("Current season: " + currentSeason.value());
            sender.sendMessage(
                "Seasonal " +
                    lowerDisplayName +
                    " growth factor: " +
                    seasonalFactor
                        .map(CropGrowthSeasonalFactor::factor)
                        .orElse(1.0) +
                    (seasonalFactor.isPresent() ? "" : " (default)")
            );
            reportBiomeVariables(sender, biomeContext.profile());
            reportSeasonVariables(sender, seasonClimate);
            CropPerformanceResult cropPerformance = reportPerformanceVariables(
                sender,
                position,
                cropKind
            );
            CropGrowthChance performanceAdjustedChance = new CropGrowthChance(
                Math.min(
                    1.0,
                    effectiveChance.value() *
                        cropPerformance.growthChanceFactor()
                )
            );
            sender.sendMessage(
                "Effective " +
                    lowerDisplayName +
                    " growth chance before performance: " +
                    effectiveChance.value()
            );
            sender.sendMessage(
                "Performance-adjusted " +
                    lowerDisplayName +
                    " growth chance: " +
                    performanceAdjustedChance.value()
            );
            sender.sendMessage(
                "May cancel natural growth: " +
                    (performanceAdjustedChance.value() < 1.0 ? "yes" : "no")
            );
            reportYieldVariables(
                sender,
                biomeContext.biomeId(),
                cropKind,
                currentSeason,
                cropPerformance.harvestQuantityFactor()
            );
        } catch (UnsupportedCropGrowthPolicyException exception) {
            sender.sendMessage(
                displayName(cropKind) + " growth policy: unsupported"
            );
            sender.sendMessage(
                "May cancel natural growth: no (vanilla fallback)"
            );
        }
        return true;
    }

    private void reportBiomeVariables(
        CommandSender sender,
        BiomeProfile profile
    ) {
        sender.sendMessage(
            "Biome humidity: " + profile.climate().humidity().normalized()
        );
        sender.sendMessage(
            "Biome temperature: " + profile.climate().temperature().normalized()
        );
        sender.sendMessage(
            "Biome soil fertility: " + profile.fertility().normalized()
        );
    }

    private void reportSeasonVariables(
        CommandSender sender,
        SeasonClimateAdjustment seasonClimate
    ) {
        sender.sendMessage(
            "Season humidity adjustment: " +
                seasonClimate.humidity().normalized()
        );
        sender.sendMessage(
            "Season humidity factor: " +
                seasonalFactor(seasonClimate.humidity())
        );
        sender.sendMessage(
            "Season temperature adjustment: " +
                seasonClimate.temperature().normalized()
        );
        sender.sendMessage(
            "Season temperature factor: " +
                seasonalFactor(seasonClimate.temperature())
        );
    }

    private CropPerformanceResult reportPerformanceVariables(
        CommandSender sender,
        BlockPosition position,
        CropKind cropKind
    ) {
        CropEnvironmentalState environmentalState =
            environmentalStateComposer.compose(position);

        try {
            CropPerformanceProfile performanceProfile =
                cropPerformanceProfileProvider.profileFor(cropKind);
            reportEnvironmentalPreference(
                sender,
                "wind speed",
                environmentalState.windSpeed(),
                performanceProfile.preferredWindSpeed()
            );
            reportEnvironmentalPreference(
                sender,
                "rain strength",
                environmentalState.rainStrength(),
                performanceProfile.preferredRainStrength()
            );
            reportEnvironmentalPreference(
                sender,
                "humidity",
                environmentalState.humidity(),
                performanceProfile.preferredHumidity()
            );
            reportEnvironmentalPreference(
                sender,
                "temperature",
                environmentalState.temperature(),
                performanceProfile.preferredTemperature()
            );
            reportEnvironmentalPreference(
                sender,
                "solar incidence",
                environmentalState.solarIncidence(),
                performanceProfile.preferredSolarIncidence()
            );
            reportEnvironmentalPreference(
                sender,
                "soil fertility",
                environmentalState.soilFertility(),
                performanceProfile.preferredSoilFertility()
            );
            CropPerformanceResult performance =
                cropPerformanceCalculator.calculate(
                    performanceProfile,
                    environmentalState
                );
            reportPerformanceResult(sender, performance);
            return performance;
        } catch (UnsupportedCropPerformanceProfileException exception) {
            sender.sendMessage(
                "Crop performance profile: unsupported (neutral fallback)"
            );
            reportPerformanceResult(sender, NEUTRAL_PERFORMANCE);
            return NEUTRAL_PERFORMANCE;
        }
    }

    private void reportYieldVariables(
        CommandSender sender,
        BiomeId biomeId,
        CropKind cropKind,
        SeasonId currentSeason,
        double harvestQuantityFactor
    ) {
        String displayName = displayName(cropKind);
        String lowerDisplayName = lowerDisplayName(cropKind);
        try {
            CropYieldPolicy yieldPolicy = cropYieldPolicyProvider.policyFor(
                biomeId
            );
            CropYieldMultiplierRange multiplierRange =
                yieldPolicy.multiplierRangeFor(cropKind);
            double seasonalFactor = yieldPolicy.seasonalFactorFor(
                cropKind,
                currentSeason
            );
            double effectiveMinimum =
                multiplierRange.minimum() *
                seasonalFactor *
                harvestQuantityFactor;
            double effectiveMaximum =
                multiplierRange.maximum() *
                seasonalFactor *
                harvestQuantityFactor;

            sender.sendMessage(displayName + " yield policy: supported");
            sender.sendMessage(
                "Configured " +
                    lowerDisplayName +
                    " yield multiplier range: " +
                    multiplierRange.minimum() +
                    "—" +
                    multiplierRange.maximum()
            );
            sender.sendMessage(
                "Seasonal " +
                    lowerDisplayName +
                    " yield factor: " +
                    seasonalFactor
            );
            sender.sendMessage(
                "Effective " +
                    lowerDisplayName +
                    " yield multiplier range: " +
                    effectiveMinimum +
                    "—" +
                    effectiveMaximum
            );
            sender.sendMessage(
                "Runtime " +
                    lowerDisplayName +
                    " yield quantity: vanilla produce quantity * selected multiplier, with probabilistic rounding for fractional results"
            );
        } catch (UnsupportedCropYieldPolicyException exception) {
            sender.sendMessage(displayName + " yield policy: unsupported");
            sender.sendMessage("Yield adjustment: no (vanilla fallback)");
        }
    }

    private static void reportEnvironmentalPreference(
        CommandSender sender,
        String variableName,
        NormalizedEnvironmentalValue actual,
        NormalizedEnvironmentalValue preferred
    ) {
        sender.sendMessage(
            "Crop environmental " +
                variableName +
                ": actual=" +
                actual.normalized() +
                ", preferred=" +
                preferred.normalized()
        );
    }

    private static void reportPerformanceResult(
        CommandSender sender,
        CropPerformanceResult performance
    ) {
        sender.sendMessage(
            "Crop performance overall score: " +
                (performance.overallScore().isPresent()
                    ? performance.overallScore().getAsDouble()
                    : "neutral fallback")
        );
        sender.sendMessage(
            "Crop growth speed factor: " +
                performance.growthSpeedFactor() +
                " (not used by natural growth listener)"
        );
        sender.sendMessage(
            "Crop growth chance factor: " + performance.growthChanceFactor()
        );
        sender.sendMessage(
            "Harvest quantity factor: " + performance.harvestQuantityFactor()
        );
    }

    private static double seasonalFactor(SeasonalAdjustment seasonAdjustment) {
        return 1.0 + seasonAdjustment.normalized();
    }

    private static String displayName(CropKind cropKind) {
        String singularName = singularize(cropKind.policyKey());
        return (
            Character.toUpperCase(singularName.charAt(0)) +
            singularName.substring(1)
        );
    }

    private static String singularize(String policyKey) {
        if (policyKey.endsWith("oes")) {
            return policyKey.substring(0, policyKey.length() - 2);
        }
        if (policyKey.endsWith("s")) {
            return policyKey.substring(0, policyKey.length() - 1);
        }
        return policyKey;
    }

    private static String lowerDisplayName(CropKind cropKind) {
        return displayName(cropKind).toLowerCase();
    }

    private static BlockPosition positionOf(Block block) {
        World world = block.getWorld();
        return new BlockPosition(
            new WorldReference(world.getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
    }
}
