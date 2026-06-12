package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicBiomes extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Dynamic Biomes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Dynamic Biomes disabled.");
    }
}
