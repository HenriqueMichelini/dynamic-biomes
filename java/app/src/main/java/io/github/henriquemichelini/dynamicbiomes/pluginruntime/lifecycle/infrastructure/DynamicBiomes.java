package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.animals.breeding.application.AnimalBreedingService;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.domain.AnimalBreedingPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure.PaperAnimalBreedingListener;
import io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure.YamlAnimalBreedingPolicyProvider;
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
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropQuantityCalculator;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.PaperOreBreakListener;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.YamlOreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.presentation.OreInspectDiagnostic;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOreMovementListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOrePlaceListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOreRemovalListener;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.YamlOreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.CachedCurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonAdvancementService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.application.SeasonInitializationService;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCycleSettings;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.SeasonAdvancementTask;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.YamlSeasonCycleSettingsProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.YamlSeasonStateRepository;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation.SeasonCommandExecutor;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.infrastructure.YamlSeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.application.TreeGrowthService;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.domain.TreeGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure.PaperTreeGrowthListener;
import io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure.YamlTreeGrowthPolicyProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class DynamicBiomes extends JavaPlugin {

    private static final List<String> CONFIGURATION_FILES = List.of(
        "ore-drops.yml",
        "crop-growth.yml",
        "crop-profiles.yml",
        "crop-yields.yml",
        "biome-profiles.yml",
        "season-profiles.yml",
        "season-cycle.yml",
        "tree-growth.yml",
        "animal-breeding.yml"
    );

    private SeasonStateRepository seasonStateRepository;
    private OreOriginTrackingService originTracking;
    private CachedCurrentSeasonQuery currentSeasonQuery;
    private List<Listener> activeListeners = List.of();
    private BukkitTask seasonTask;

    @Override
    public void onEnable() {
        Path dataPath = getDataFolder().toPath();
        for (String file : CONFIGURATION_FILES) {
            if (Files.notExists(dataPath.resolve(file))) {
                saveResource(file, false);
            }
        }
        seasonStateRepository = new YamlSeasonStateRepository(dataPath.resolve("season-state.yml"));
        originTracking = new OreOriginTrackingService(
            new YamlOreOriginRepository(dataPath.resolve("ore-origins.yml"))
        );
        install(buildRuntime(dataPath));
        getLogger().info("Dynamic Biomes enabled.");
    }

    private boolean reloadConfiguration() {
        try {
            install(buildRuntime(getDataFolder().toPath()));
            return true;
        } catch (RuntimeException exception) {
            getLogger().log(Level.WARNING, "Could not reload DynamicBiomes configuration.", exception);
            return false;
        }
    }

    private RuntimeConfiguration buildRuntime(Path dataPath) {
        SeasonCycleSettings cycleSettings = new YamlSeasonCycleSettingsProvider(
            dataPath.resolve("season-cycle.yml")
        ).settings();
        SeasonCalendar seasonCalendar = cycleSettings.calendar();

        BiomeProfileProvider biomeProfileProvider = new YamlBiomeProfileProvider(
            dataPath.resolve("biome-profiles.yml")
        ).load();

        BiomeResolver biomeResolver = new BukkitBiomeResolver(
            getServer(),
            biomeProfileProvider
        );

        OreDropPolicyProvider oreDropPolicyProvider = new YamlOreDropPolicyProvider(
            dataPath.resolve("ore-drops.yml")
        ).load();

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
        ).load();
        seasonCalendar.seasons().forEach(seasonProfileProvider::profileFor);

        TreeGrowthPolicyProvider treeGrowthPolicyProvider = new YamlTreeGrowthPolicyProvider(
            dataPath.resolve("tree-growth.yml"),
            Math::random
        );
        AnimalBreedingPolicyProvider animalBreedingPolicyProvider =
            new YamlAnimalBreedingPolicyProvider(dataPath.resolve("animal-breeding.yml"));

        CachedCurrentSeasonQuery seasonQuery = seasonQueryFor(seasonCalendar);
        SeasonAdvancementService seasonAdvancement = new SeasonAdvancementService(
            seasonCalendar,
            seasonStateRepository,
            seasonQuery
        );
        CropEnvironmentalStateComposer cropEnvironmentalStateComposer =
            new CropEnvironmentalStateComposer(biomeResolver, seasonQuery, seasonProfileProvider);
        CropPerformanceCalculator cropPerformanceCalculator = new CropPerformanceCalculator();

        OreDropService oreDropService = new OreDropService(
            originTracking,
            biomeResolver,
            oreDropPolicyProvider,
            seasonQuery,
            new OreDropMultiplierCalculator(Math::random),
            new OreDropQuantityCalculator(Math::random)
        );

        CropPerformanceService cropPerformanceService = new CropPerformanceService(
            cropEnvironmentalStateComposer,
            cropPerformanceProfileProvider,
            cropPerformanceCalculator
        );

        CropGrowthService cropGrowthService = new CropGrowthService(
            biomeResolver,
            cropGrowthPolicyProvider,
            seasonQuery,
            cropPerformanceService::performanceFor
        );

        CropYieldService cropYieldService = new CropYieldService(
            biomeResolver,
            cropYieldPolicyProvider,
            seasonQuery,
            cropPerformanceService::performanceFor,
            new CropYieldMultiplierCalculator(Math::random),
            new CropYieldQuantityCalculator(Math::random)
        );

        DynamicBiomesCommandExecutor command = new DynamicBiomesCommandExecutor(
            new SeasonCommandExecutor(seasonQuery, seasonAdvancement::setCurrentSeason),
            new BiomeCommandExecutor(biomeResolver),
            new DynamicBiomesInspectCommandExecutor(
                List.of(
                    new CropGrowthInspectDiagnostic(
                        biomeResolver,
                        cropGrowthPolicyProvider,
                        seasonQuery,
                        seasonProfileProvider,
                        cropEnvironmentalStateComposer,
                        cropPerformanceProfileProvider,
                        cropPerformanceCalculator,
                        cropYieldPolicyProvider
                    )::inspect,
                    new OreInspectDiagnostic(
                        new OreInspectionService(biomeResolver, oreDropPolicyProvider, originTracking)
                    )::inspect
                )
            ),
            this::reloadConfiguration
        );

        List<Listener> listeners = List.of(
            new PaperOrePlaceListener(originTracking),
            new PaperOreBreakListener(oreDropService),
            new PaperOreRemovalListener(
                originTracking,
                task -> getServer().getScheduler().runTask(this, task)
            ),
            new PaperOreMovementListener(originTracking),
            new PaperCropGrowthListener(cropGrowthService),
            new PaperCropHarvestListener(cropYieldService),
            new PaperTreeGrowthListener(
                new TreeGrowthService(biomeResolver, treeGrowthPolicyProvider, seasonQuery)
            ),
            new PaperAnimalBreedingListener(
                new AnimalBreedingService(animalBreedingPolicyProvider, seasonQuery, Math::random)
            )
        );

        return new RuntimeConfiguration(command, listeners, seasonQuery, cycleSettings, seasonAdvancement);
    }

    private CachedCurrentSeasonQuery seasonQueryFor(SeasonCalendar calendar) {
        if (currentSeasonQuery == null) {
            SeasonId initialSeason = new SeasonInitializationService(calendar, seasonStateRepository)
                .initializeIfMissing();
            return new CachedCurrentSeasonQuery(initialSeason);
        }
        SeasonId currentSeason = currentSeasonQuery.currentSeason();
        if (!calendar.contains(currentSeason)) {
            throw new IllegalArgumentException(
                "Configured calendar must include the current season: " + currentSeason.value()
            );
        }
        return currentSeasonQuery;
    }

    private void install(RuntimeConfiguration configuration) {
        PluginCommand command = Objects.requireNonNull(
            getCommand("dynamicbiomes"),
            "Missing dynamicbiomes command metadata"
        );
        BukkitTask nextTask = null;
        try {
            for (Listener listener : configuration.listeners()) {
                getServer().getPluginManager().registerEvents(listener, this);
            }
            SeasonCycleSettings settings = configuration.cycleSettings();
            if (settings.enabled()) {
                nextTask = getServer().getScheduler().runTaskTimer(
                    this,
                    new SeasonAdvancementTask(configuration.seasonAdvancement()),
                    settings.initialDelayTicks(),
                    settings.intervalTicks()
                );
            }
            command.setExecutor(configuration.command());
        } catch (RuntimeException exception) {
            configuration.listeners().forEach(HandlerList::unregisterAll);
            if (nextTask != null) {
                nextTask.cancel();
            }
            throw exception;
        }

        unregisterRuntime();
        activeListeners = configuration.listeners();
        seasonTask = nextTask;
        currentSeasonQuery = configuration.seasonQuery();
    }

    private void unregisterRuntime() {
        activeListeners.forEach(HandlerList::unregisterAll);
        activeListeners = List.of();
        if (seasonTask != null) {
            seasonTask.cancel();
            seasonTask = null;
        }
    }

    @Override
    public void onDisable() {
        unregisterRuntime();
        currentSeasonQuery = null;
        getLogger().info("Dynamic Biomes disabled.");
    }

    private record RuntimeConfiguration(
        DynamicBiomesCommandExecutor command,
        List<Listener> listeners,
        CachedCurrentSeasonQuery seasonQuery,
        SeasonCycleSettings cycleSettings,
        SeasonAdvancementService seasonAdvancement
    ) {}
}
