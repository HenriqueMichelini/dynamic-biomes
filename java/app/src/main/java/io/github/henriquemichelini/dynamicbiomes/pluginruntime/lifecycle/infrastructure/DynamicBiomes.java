package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure.YamlBiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure.BukkitBiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.presentation.BiomeCommandExecutor;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.application.CropGrowthService;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.PaperCropGrowthListener;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.YamlCropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation.CropGrowthInspectDiagnostic;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.CropEnvironmentalStateComposer;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.CropPerformanceService;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.infrastructure.YamlCropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.application.CropYieldService;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure.PaperCropHarvestListener;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure.YamlCropYieldPolicyProvider;
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
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.infrastructure.YamlSeasonProfileProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class DynamicBiomes extends JavaPlugin {

    @Override
    public void onEnable() {
        saveResource("ore-drops.yml", false);
        saveResource("crop-growth.yml", false);
        saveResource("crop-profiles.yml", false);
        saveResource("crop-yields.yml", false);
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

        CropGrowthPolicyProvider cropGrowthPolicyProvider =
            new YamlCropGrowthPolicyProvider(
                dataPath.resolve("crop-growth.yml"),
                Math::random
            );

        CropYieldPolicyProvider cropYieldPolicyProvider =
            new YamlCropYieldPolicyProvider(
                dataPath.resolve("crop-yields.yml")
            );

        CropPerformanceProfileProvider cropPerformanceProfileProvider =
            new YamlCropPerformanceProfileProvider(
                dataPath.resolve("crop-profiles.yml")
            );

        SeasonProfileProvider seasonProfileProvider = new YamlSeasonProfileProvider(
            dataPath.resolve("season-profiles.yml")
        );

        CropEnvironmentalStateComposer cropEnvironmentalStateComposer =
            new CropEnvironmentalStateComposer(
                biomeResolver,
                currentSeasonQuery,
                seasonProfileProvider
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
                        new CropGrowthInspectDiagnostic(
                            biomeResolver,
                            cropGrowthPolicyProvider,
                            currentSeasonQuery,
                            seasonProfileProvider,
                            cropEnvironmentalStateComposer,
                            cropPerformanceProfileProvider,
                            new CropPerformanceCalculator(),
                            cropYieldPolicyProvider
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

        CropPerformanceService cropPerformanceService = new CropPerformanceService(
            cropEnvironmentalStateComposer,
            cropPerformanceProfileProvider,
            new CropPerformanceCalculator()
        );

        CropGrowthService cropGrowthService = new CropGrowthService(
            biomeResolver,
            cropGrowthPolicyProvider,
            currentSeasonQuery,
            cropPerformanceService::performanceFor
        );

        CropYieldService cropYieldService = new CropYieldService(
            biomeResolver,
            cropYieldPolicyProvider,
            currentSeasonQuery,
            cropPerformanceService::performanceFor,
            new CropYieldMultiplierCalculator(Math::random),
            new CropYieldQuantityCalculator(Math::random)
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
            new PaperCropGrowthListener(cropGrowthService),
            this
        );

        getServer().getPluginManager().registerEvents(
            new PaperCropHarvestListener(cropYieldService),
            this
        );

        getLogger().info("Dynamic Biomes enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Dynamic Biomes disabled.");
    }
}
