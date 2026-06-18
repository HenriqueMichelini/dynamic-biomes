package io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.infrastructure.PaperCropMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Optional;
import lombok.NonNull;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

public final class CropGrowthInspectDiagnostic {

    private final BiomeResolver biomeResolver;
    private final CropGrowthPolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;

    public CropGrowthInspectDiagnostic(
        @NonNull BiomeResolver biomeResolver,
        @NonNull CropGrowthPolicyProvider policyProvider,
        @NonNull CurrentSeasonQuery currentSeasonQuery
    ) {
        this.biomeResolver = biomeResolver;
        this.policyProvider = policyProvider;
        this.currentSeasonQuery = currentSeasonQuery;
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
            sender.sendMessage(
                "Effective " +
                    lowerDisplayName +
                    " growth chance: " +
                    effectiveChance.value()
            );
            sender.sendMessage(
                "May cancel natural growth: " +
                    (effectiveChance.value() < 1.0 ? "yes" : "no")
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
