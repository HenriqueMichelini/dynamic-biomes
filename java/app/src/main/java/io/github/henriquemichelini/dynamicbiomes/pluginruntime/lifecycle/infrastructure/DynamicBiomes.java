package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure.YamlBiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure.BukkitBiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.presentation.BiomeCommandExecutor;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.WheatGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.PaperWheatGrowthListener;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.YamlWheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation.WheatGrowthInspectDiagnostic;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreDropService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.PaperOreBreakListener;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.YamlOreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.presentation.OreInspectCommandExecutor;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOreMovementListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOrePlaceListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.YamlOreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.CachedCurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonAdvancementService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonInitializationService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCycleSettings;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.SeasonAdvancementTask;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.YamlSeasonCycleSettingsProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.YamlSeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation.SeasonCommandExecutor;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicBiomes extends JavaPlugin {

    @Override
    public void onEnable() {
        saveResource("ore-drops.yml", false);
        saveResource("crop-growth.yml", false);
        saveResource("biome-profiles.yml", false);
        saveResource("season-profiles.yml", false);
        saveResource("season-cycle.yml", false);

        Path dataPath = getDataFolder().toPath();

        SeasonCalendar seasonCalendar = new SeasonCalendar(
            List.of(
                new SeasonId("minecraft:spring"),
                new SeasonId("minecraft:summer"),
                new SeasonId("minecraft:autumn"),
                new SeasonId("minecraft:winter")
            )
        );

        YamlSeasonStateRepository seasonStateRepository = new YamlSeasonStateRepository(
            dataPath.resolve("season-state.yml")
        );

        SeasonInitializationService seasonInitialization = new SeasonInitializationService(
            seasonCalendar,
            seasonStateRepository
        );

        SeasonId initialSeason = seasonInitialization.initializeIfMissing();
        CachedCurrentSeasonQuery currentSeasonQuery = new CachedCurrentSeasonQuery(
            initialSeason
        );

        SeasonCycleSettings cycleSettings = new YamlSeasonCycleSettingsProvider(
            dataPath.resolve("season-cycle.yml")
        ).settings();

        if (cycleSettings.enabled()) {
            getServer().getScheduler().runTaskTimer(
                this,
                new SeasonAdvancementTask(
                    new SeasonAdvancementService(
                        seasonCalendar,
                        seasonStateRepository,
                        currentSeasonQuery
                    )
                ),
                cycleSettings.initialDelayTicks(),
                cycleSettings.intervalTicks()
            );
        }

        YamlOreOriginRepository oreOriginRepository = new YamlOreOriginRepository(
            dataPath.resolve("ore-origins.yml")
        );
        OreOriginTrackingService originTracking = new OreOriginTrackingService(
            oreOriginRepository
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

        WheatGrowthChancePolicyProvider wheatGrowthPolicyProvider =
            new YamlWheatGrowthChancePolicyProvider(
                dataPath.resolve("crop-growth.yml"),
                Math::random
            );

        Objects.requireNonNull(
            getCommand("dynamicbiomes"),
            "Missing dynamicbiomes command metadata"
        ).setExecutor(
            new DynamicBiomesCommandExecutor(
                new SeasonCommandExecutor(currentSeasonQuery),
                new BiomeCommandExecutor(biomeResolver),
                new DynamicBiomesInspectCommandExecutor(
                    List.of(
                        new WheatGrowthInspectDiagnostic(
                            biomeResolver,
                            wheatGrowthPolicyProvider
                        )::inspect,
                        new OreInspectCommandExecutor(
                            biomeResolver,
                            oreDropPolicyProvider,
                            originTracking
                        )::inspect
                    )
                )
            )
        );

        OreDropService oreDropService = new OreDropService(
            originTracking,
            biomeResolver,
            oreDropPolicyProvider,
            currentSeasonQuery,
            new OreDropMultiplierCalculator(Math::random),
            new OreDropQuantityCalculator(Math::random)
        );

        WheatGrowthService wheatGrowthService = new WheatGrowthService(
            biomeResolver,
            wheatGrowthPolicyProvider
        );

        getServer().getPluginManager().registerEvents(
            new PaperOrePlaceListener(originTracking),
            this
        );

        getServer().getPluginManager().registerEvents(
            new PaperOreBreakListener(oreDropService, originTracking),
            this
        );

        getServer().getPluginManager().registerEvents(
            new PaperOreMovementListener(originTracking),
            this
        );

        getServer().getPluginManager().registerEvents(
            new PaperWheatGrowthListener(wheatGrowthService),
            this
        );

        getLogger().info("Dynamic Biomes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Dynamic Biomes disabled.");
    }
}
