package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure.YamlBiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure.BukkitBiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.PaperOreBreakListener;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.YamlOreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOrePlaceListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.YamlOreOriginRepository;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicBiomes extends JavaPlugin {

    @Override
    public void onEnable() {
        saveResource("ore-drops.yml", false);
        saveResource("biome-profiles.yml", false);

        Path dataPath = getDataFolder().toPath();
        OreOriginTrackingService originTracking = new OreOriginTrackingService(
            new YamlOreOriginRepository(dataPath.resolve("ore-origins.yml"))
        );
        BiomeProfileProvider biomeProfileProvider = new YamlBiomeProfileProvider(
            dataPath.resolve("biome-profiles.yml")
        );
        BiomeResolver biomeResolver = new BukkitBiomeResolver(
            getServer(),
            biomeProfileProvider
        );
        OreDropPolicyProvider oreDropPolicyProvider = new YamlOreDropPolicyProvider(
            dataPath.resolve("ore-drops.yml")
        );
        OreDropService oreDropService = new OreDropService(
            originTracking,
            biomeResolver,
            oreDropPolicyProvider,
            new OreDropMultiplierCalculator(Math::random),
            new OreDropQuantityCalculator(Math::random)
        );

        getServer().getPluginManager().registerEvents(
            new PaperOrePlaceListener(originTracking),
            this
        );
        getServer().getPluginManager().registerEvents(
            new PaperOreBreakListener(oreDropService, originTracking),
            this
        );

        getLogger().info("Dynamic Biomes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Dynamic Biomes disabled.");
    }
}
