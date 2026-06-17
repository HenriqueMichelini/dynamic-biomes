package io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedWheatGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

public final class WheatGrowthInspectDiagnostic {
    private final BiomeResolver biomeResolver;
    private final WheatGrowthChancePolicyProvider policyProvider;
    private final CurrentSeasonQuery currentSeasonQuery;

    public WheatGrowthInspectDiagnostic(
        BiomeResolver biomeResolver,
        WheatGrowthChancePolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery
    ) {
        this.biomeResolver = Objects.requireNonNull(biomeResolver);
        this.policyProvider = Objects.requireNonNull(policyProvider);
        this.currentSeasonQuery = Objects.requireNonNull(currentSeasonQuery);
    }

    public boolean inspect(CommandSender sender, Block targetBlock) {
        if (targetBlock.getType() != Material.WHEAT) {
            return false;
        }

        BlockPosition position = positionOf(targetBlock);
        BiomeContext biomeContext;
        try {
            biomeContext = biomeResolver.resolve(position);
        } catch (UnsupportedBiomeException exception) {
            String biomeId = exception.biomeId()
                .map(BiomeId::value)
                .orElse("unknown");
            sender.sendMessage("Current biome: " + biomeId);
            sender.sendMessage("DynamicBiomes profile: unsupported");
            sender.sendMessage("Wheat growth policy: unsupported");
            sender.sendMessage("May cancel natural growth: no (vanilla fallback)");
            return true;
        }

        sender.sendMessage("Current biome: " + biomeContext.biomeId().value());
        sender.sendMessage("DynamicBiomes profile: supported");

        try {
            WheatGrowthChancePolicy policy = policyProvider.policyFor(
                biomeContext.biomeId()
            );
            WheatGrowthChance configuredChance = policy.configuredChance();
            SeasonId currentSeason = currentSeasonQuery.currentSeason();
            Optional<WheatGrowthSeasonalFactor> seasonalFactor =
                policy.seasonalFactorFor(currentSeason);
            WheatGrowthChance effectiveChance = policy.effectiveChanceFor(
                currentSeason
            );
            sender.sendMessage("Wheat growth policy: supported");
            sender.sendMessage(
                "Configured wheat growth chance: " + configuredChance.value()
            );
            sender.sendMessage("Current season: " + currentSeason.value());
            sender.sendMessage(
                "Seasonal wheat growth factor: " +
                    seasonalFactor.map(WheatGrowthSeasonalFactor::factor).orElse(1.0) +
                    (seasonalFactor.isPresent() ? "" : " (default)")
            );
            sender.sendMessage(
                "Effective wheat growth chance: " + effectiveChance.value()
            );
            sender.sendMessage(
                "May cancel natural growth: " +
                    (effectiveChance.value() < 1.0 ? "yes" : "no")
            );
        } catch (UnsupportedWheatGrowthPolicyException exception) {
            sender.sendMessage("Wheat growth policy: unsupported");
            sender.sendMessage("May cancel natural growth: no (vanilla fallback)");
        }
        return true;
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
