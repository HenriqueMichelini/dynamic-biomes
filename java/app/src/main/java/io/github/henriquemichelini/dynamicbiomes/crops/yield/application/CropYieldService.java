package io.github.henriquemichelini.dynamicbiomes.crops.yield.application;

import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldBiomeFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldClimateFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEffectiveMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEnvironmentalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldEnvironmentalFactorCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.UnsupportedCropYieldPolicyException;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;

public final class CropYieldService {
    private final BiomeResolver biomeResolver;
    private final CropYieldPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;
    private final SeasonProfileProvider seasonProfileProvider;
    private final CropYieldMultiplierCalculator multiplierCalculator;
    private final CropYieldQuantityCalculator quantityCalculator;
    private final CropYieldBiomeFactorCalculator biomeFactorCalculator;
    private final CropYieldClimateFactorCalculator climateFactorCalculator;
    private final CropYieldEnvironmentalFactorCalculator environmentalFactorCalculator;
    private final CropYieldEffectiveMultiplierCalculator effectiveMultiplierCalculator;
    private final boolean environmentalFactorsEnabled;

    public CropYieldService(
        BiomeResolver biomeResolver,
        CropYieldPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        CropYieldMultiplierCalculator multiplierCalculator,
        CropYieldQuantityCalculator quantityCalculator
    ) {
        this(
            biomeResolver,
            policyProvider,
            currentSeasonQuery,
            CropYieldService::neutralSeasonProfile,
            multiplierCalculator,
            quantityCalculator,
            new CropYieldBiomeFactorCalculator(),
            new CropYieldClimateFactorCalculator(),
            new CropYieldEnvironmentalFactorCalculator(),
            new CropYieldEffectiveMultiplierCalculator(),
            false
        );
    }

    public CropYieldService(
        BiomeResolver biomeResolver,
        CropYieldPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider,
        CropYieldMultiplierCalculator multiplierCalculator,
        CropYieldQuantityCalculator quantityCalculator,
        CropYieldBiomeFactorCalculator biomeFactorCalculator,
        CropYieldClimateFactorCalculator climateFactorCalculator,
        CropYieldEnvironmentalFactorCalculator environmentalFactorCalculator,
        CropYieldEffectiveMultiplierCalculator effectiveMultiplierCalculator
    ) {
        this(
            biomeResolver,
            policyProvider,
            currentSeasonQuery,
            seasonProfileProvider,
            multiplierCalculator,
            quantityCalculator,
            biomeFactorCalculator,
            climateFactorCalculator,
            environmentalFactorCalculator,
            effectiveMultiplierCalculator,
            true
        );
    }

    private CropYieldService(
        BiomeResolver biomeResolver,
        CropYieldPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider,
        CropYieldMultiplierCalculator multiplierCalculator,
        CropYieldQuantityCalculator quantityCalculator,
        CropYieldBiomeFactorCalculator biomeFactorCalculator,
        CropYieldClimateFactorCalculator climateFactorCalculator,
        CropYieldEnvironmentalFactorCalculator environmentalFactorCalculator,
        CropYieldEffectiveMultiplierCalculator effectiveMultiplierCalculator,
        boolean environmentalFactorsEnabled
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
        this.seasonProfileProvider = seasonProfileProvider;
        this.multiplierCalculator = multiplierCalculator;
        this.quantityCalculator = quantityCalculator;
        this.biomeFactorCalculator = biomeFactorCalculator;
        this.climateFactorCalculator = climateFactorCalculator;
        this.environmentalFactorCalculator = environmentalFactorCalculator;
        this.effectiveMultiplierCalculator = effectiveMultiplierCalculator;
        this.environmentalFactorsEnabled = environmentalFactorsEnabled;
    }

    public int calculateProduceQuantity(
        BlockPosition position,
        CropKind cropKind,
        int vanillaProduceQuantity
    ) {
        try {
            BiomeContext biomeContext = biomeResolver.resolve(position);
            CropYieldPolicy policy = policyProvider.policyFor(biomeContext.biomeId());
            double selectedMultiplier = multiplierCalculator.calculate(
                policy.multiplierRangeFor(cropKind)
            );
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            CropYieldSeasonalFactor seasonalFactor = new CropYieldSeasonalFactor(
                policy.seasonalFactorFor(cropKind, currentSeason)
            );
            double effectiveMultiplier = environmentalFactorsEnabled
                ? calculateOptionBMultiplier(
                    biomeContext,
                    currentSeason,
                    selectedMultiplier,
                    seasonalFactor
                )
                : selectedMultiplier * seasonalFactor.factor();

            return quantityCalculator.calculate(
                vanillaProduceQuantity,
                effectiveMultiplier
            );
        } catch (
            UnsupportedBiomeException
            | UnsupportedCropYieldPolicyException e
        ) {
            return vanillaProduceQuantity;
        }
    }

    private double calculateOptionBMultiplier(
        BiomeContext biomeContext,
        SeasonId currentSeason,
        double selectedMultiplier,
        CropYieldSeasonalFactor seasonalFactor
    ) {
        SeasonProfile currentSeasonProfile = seasonProfileProvider.profileFor(
            currentSeason
        );
        CropYieldEnvironmentalFactor biomeYieldFactor =
            biomeFactorCalculator.calculate(biomeContext.profile().fertility());
        CropYieldEnvironmentalFactor climateYieldFactor =
            climateFactorCalculator.calculate(
                currentSeasonProfile.climateAdjustment()
            );
        CropYieldEnvironmentalFactor environmentalFactor =
            environmentalFactorCalculator.calculate(
                biomeYieldFactor,
                climateYieldFactor
            );
        return effectiveMultiplierCalculator.calculate(
            selectedMultiplier,
            seasonalFactor,
            environmentalFactor
        );
    }

    private static SeasonProfile neutralSeasonProfile(SeasonId seasonId) {
        return new SeasonProfile(
            seasonId,
            new SeasonClimateAdjustment(
                new SeasonalAdjustment(0.0),
                new SeasonalAdjustment(0.0)
            )
        );
    }
}
