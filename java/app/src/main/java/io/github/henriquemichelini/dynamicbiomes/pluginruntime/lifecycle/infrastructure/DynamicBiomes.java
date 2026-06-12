package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.PaperOreBreakListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOrePlaceListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.YamlOreOriginRepository;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicBiomes extends JavaPlugin {

    @Override
    public void onEnable() {
        OreOriginTrackingService originTracking = new OreOriginTrackingService(
            new YamlOreOriginRepository(
                getDataFolder().toPath().resolve("ore-origins.yml")
            )
        );

        getServer().getPluginManager().registerEvents(
            new PaperOrePlaceListener(originTracking),
            this
        );
        getServer().getPluginManager().registerEvents(
            new PaperOreBreakListener(originTracking),
            this
        );

        getLogger().info("Dynamic Biomes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Dynamic Biomes disabled.");
    }
}
