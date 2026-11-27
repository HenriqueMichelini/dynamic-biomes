package io.github.henriquemichelini.dynamicbiomes.pluginruntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.jar.JarFile;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.CommandResult;
import org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock;
import org.mockbukkit.mockbukkit.scheduler.RepeatingTask;
import org.mockbukkit.mockbukkit.scheduler.ScheduledTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicBiomesPluginTest {
    private static final String SEASON_ADVANCEMENT_TASK_CLASS_NAME =
        "io.github.henriquemichelini.dynamicbiomes.seasons.cycle.infrastructure.SeasonAdvancementTask";

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void loadsEnablesAndDisablesThePackagedPlugin() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        assertTrue(plugin.isEnabled());
        assertEquals(
            "io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure.DynamicBiomes",
            plugin.getClass().getName()
        );

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());
    }

    @Test
    void packagedPluginKeepsItsExactLifecycleLogMessages() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        MessageRecordingHandler handler = new MessageRecordingHandler();
        plugin.getLogger().addHandler(handler);

        server.getPluginManager().enablePlugin(plugin);
        plugin.onDisable();

        assertEquals(
            List.of("Dynamic Biomes enabled.", "Dynamic Biomes disabled."),
            handler.messages
        );
    }

    @Test
    void packagedPluginKeepsItsResourceContract() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));

        try (JarFile pluginJar = new JarFile(System.getProperty("dynamicBiomes.pluginJar"))) {
            assertNotNull(pluginJar.getEntry("plugin.yml"));
            assertNotNull(pluginJar.getEntry("ore-drops.yml"));
            assertNotNull(pluginJar.getEntry("crop-growth.yml"));
            assertNotNull(pluginJar.getEntry("crop-yields.yml"));
            assertNotNull(pluginJar.getEntry("crop-profiles.yml"));
            assertNotNull(pluginJar.getEntry("biome-profiles.yml"));
            assertNotNull(pluginJar.getEntry("season-profiles.yml"));
            assertNotNull(pluginJar.getEntry("season-cycle.yml"));
            assertNotNull(pluginJar.getEntry("tree-growth.yml"));
            assertNotNull(pluginJar.getEntry("animal-breeding.yml"));
            assertNull(pluginJar.getEntry("biomes.yml"));
            assertEquals(System.getProperty("dynamicBiomes.pluginVersion"), plugin.getPluginMeta().getVersion());
            assertEquals(
                "io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure.DynamicBiomes",
                plugin.getPluginMeta().getMainClass()
            );
        }
    }

    @Test
    void savesDefaultRuntimeConfigurationResourcesOnEnable() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        Path dataFolder = plugin.getDataFolder().toPath();

        assertTrue(Files.exists(dataFolder.resolve("ore-drops.yml")));
        assertTrue(Files.exists(dataFolder.resolve("crop-growth.yml")));
        assertTrue(Files.exists(dataFolder.resolve("crop-yields.yml")));
        assertTrue(Files.exists(dataFolder.resolve("crop-profiles.yml")));
        assertTrue(Files.exists(dataFolder.resolve("biome-profiles.yml")));
        assertTrue(Files.exists(dataFolder.resolve("season-profiles.yml")));
        assertTrue(Files.exists(dataFolder.resolve("season-cycle.yml")));
        assertTrue(Files.exists(dataFolder.resolve("tree-growth.yml")));
        assertTrue(Files.exists(dataFolder.resolve("animal-breeding.yml")));
    }

    @Test
    void registersRuntimeListenersOnEnable() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        List<String> listenerClassNames = HandlerList.getRegisteredListeners(plugin)
            .stream()
            .map(registeredListener -> registeredListener.getListener().getClass().getName())
            .toList();

        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOrePlaceListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure.PaperOreBreakListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.PaperOreMovementListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.PaperCropGrowthListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.crops.yield.infrastructure.PaperCropHarvestListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.trees.growth.infrastructure.PaperTreeGrowthListener"
        ));
        assertTrue(listenerClassNames.contains(
            "io.github.henriquemichelini.dynamicbiomes.animals.breeding.infrastructure.PaperAnimalBreedingListener"
        ));
    }

    @Test
    void destructiveExplosionClearsOnlyTheOriginsInItsFinalBlockList() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        UUID worldId = UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e");
        World world = paperEventProxy(World.class, Map.of("getUID", worldId));
        Block explodedOre = paperEventProxy(Block.class, Map.of(
            "getType", Material.IRON_ORE, "getWorld", world, "getX", 10, "getY", 64, "getZ", -20
        ));
        Path origins = plugin.getDataFolder().toPath().resolve("ore-origins.yml");
        String originalOrigins = """
            origins:
              - world: %1$s
                x: 10
                y: 64
                z: -20
                type: PLAYER_PLACED
              - world: %1$s
                x: 11
                y: 64
                z: -20
                type: PLAYER_PLACED
            """.formatted(worldId);
        Files.writeString(origins, originalOrigins);
        Entity entity = paperEventProxy(Entity.class, Map.of());
        Location location = new Location(world, 10, 64, -20);
        var cancelled = new EntityExplodeEvent(
            entity, location, List.of(explodedOre), 0.0f, ExplosionResult.DESTROY
        );
        cancelled.setCancelled(true);
        var triggerOnly = new EntityExplodeEvent(
            entity, location, List.of(explodedOre), 0.0f, ExplosionResult.TRIGGER_BLOCK
        );
        server.getPluginManager().callEvent(cancelled);
        server.getPluginManager().callEvent(triggerOnly);
        assertEquals(originalOrigins, Files.readString(origins));

        server.getPluginManager().callEvent(new EntityExplodeEvent(
            entity, location, List.of(explodedOre), 0.0f, ExplosionResult.DESTROY_WITH_DECAY
        ));

        String persistedOrigins = Files.readString(origins);
        assertFalse(persistedOrigins.contains("x: 10"));
        assertTrue(persistedOrigins.contains("x: 11"));
    }

    private static <T> T paperEventProxy(Class<T> type, Map<String, Object> returnValues) {
        return type.cast(Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, arguments) -> returnValues.get(method.getName())
        ));
    }

    @Test
    void registersAndExecutesTheCurrentSeasonCommand() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        assertNotNull(server.getPluginCommand("dynamicbiomes"));

        CommandResult result = server.executeConsole("dynamicbiomes", "season");

        assertTrue(result.hasSucceeded());
        assertEquals(
            "Current season: minecraft:spring",
            result.getSender().nextMessage()
        );
    }

    @Test
    void diagnosticsAllowConsoleAndDefaultToOperatorsWithoutLoadingOrigins() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        Path origins = plugin.getDataFolder().toPath().resolve("ore-origins.yml");
        Files.writeString(origins, "invalid ore origin data");

        CommandResult result = server.executeConsole("db", "diagnostics");
        assertTrue(result.hasSucceeded());
        var console = result.getSender();
        assertTrue(console.nextMessage().matches(
            "Ore-origin write measurement window: [0-9]+\\.[0-9]{3} s since plugin startup"
        ));
        assertEquals("Ore-origin writes: 0 attempts, 0 succeeded, 0 failed", console.nextMessage());
        assertEquals("Approximate p95 write duration: no data (no write attempts)", console.nextMessage());
        assertEquals("Tracked ore origins: not loaded", console.nextMessage());
        assertEquals("invalid ore origin data", Files.readString(origins));

        var permission = plugin.getPluginMeta().getPermissions().stream()
            .filter(entry -> entry.getName().equals("dynamicbiomes.admin.diagnostics"))
            .findFirst()
            .orElseThrow();
        assertEquals(PermissionDefault.OP, permission.getDefault());
    }

    @Test
    void diagnosticsKeepObservationsAcrossReloadAndResetOnPluginRestart() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        callOrePlacement(10);
        assertDiagnosticsWithWrites(1);

        assertEquals(
            "DynamicBiomes configuration reloaded.",
            server.executeConsole("db", "reload").getSender().nextMessage()
        );
        assertDiagnosticsWithWrites(1);
        callOrePlacement(11);
        assertDiagnosticsWithWrites(2);

        server.getPluginManager().disablePlugin(plugin);
        server.getPluginManager().enablePlugin(plugin);
        var console = server.executeConsole("db", "diagnostics").getSender();
        assertTrue(console.nextMessage().startsWith("Ore-origin write measurement window:"));
        assertEquals("Ore-origin writes: 0 attempts, 0 succeeded, 0 failed", console.nextMessage());
        assertEquals("Approximate p95 write duration: no data (no write attempts)", console.nextMessage());
        assertEquals("Tracked ore origins: not loaded", console.nextMessage());
    }

    private void callOrePlacement(int x) {
        UUID worldId = UUID.fromString("fdab89dd-8aac-4be0-9c26-8752ae6ce85e");
        World world = paperEventProxy(World.class, Map.of("getUID", worldId));
        Block ore = paperEventProxy(Block.class, Map.of(
            "getType", Material.IRON_ORE, "getWorld", world, "getX", x, "getY", 64, "getZ", -20
        ));
        server.getPluginManager().callEvent(new BlockPlaceEvent(
            ore, null, null, null, null, true, EquipmentSlot.HAND
        ));
    }

    private void assertDiagnosticsWithWrites(int count) {
        var console = server.executeConsole("db", "diagnostics").getSender();
        assertTrue(console.nextMessage().startsWith("Ore-origin write measurement window:"));
        assertEquals(
            "Ore-origin writes: %d attempts, %d succeeded, 0 failed".formatted(count, count),
            console.nextMessage()
        );
        assertTrue(console.nextMessage().matches(
            "Approximate p95 write duration: <= [0-9]+\\.[0-9]{3} ms"
        ));
        assertEquals("Tracked ore origins: " + count, console.nextMessage());
    }

    @Test
    void reloadAppliesNewScheduleWithoutResettingTheCurrentSeason() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        Path cycle = plugin.getDataFolder().toPath().resolve("season-cycle.yml");
        Files.writeString(cycle, """
            advancement:
              enabled: true
              initial-delay-ticks: 5
              interval-ticks: 7
            """);
        CommandResult setSeason = server.executeConsole("dynamicbiomes", "season", "set", "minecraft:autumn");
        assertTrue(setSeason.hasSucceeded());
        assertEquals("Current season: minecraft:autumn", setSeason.getSender().nextMessage());

        CommandResult reload = server.executeConsole("dynamicbiomes", "reload");

        assertEquals("DynamicBiomes configuration reloaded.", reload.getSender().nextMessage());
        assertEquals("Current season: minecraft:autumn", currentSeason());
        assertEquals(1, seasonAdvancementTasks().size());
        server.getScheduler().performTicks(5);
        assertEquals("Current season: minecraft:winter", currentSeason());

        Files.writeString(cycle, """
            advancement:
              enabled: false
              initial-delay-ticks: 5
              interval-ticks: 7
            """);
        assertEquals(
            "DynamicBiomes configuration reloaded.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        server.getScheduler().performTicks(14);
        assertEquals("Current season: minecraft:winter", currentSeason());
    }

    @Test
    void badConfigurationLeavesTheActiveListenersAndScheduleInPlace() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        Path dataFolder = plugin.getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        Files.writeString(dataFolder.resolve("season-cycle.yml"), """
            advancement:
              enabled: true
              initial-delay-ticks: 5
              interval-ticks: 7
            """);
        server.getPluginManager().enablePlugin(plugin);
        List<Listener> activeListeners = listenersFor(plugin);
        List<RepeatingTask> activeTasks = seasonAdvancementTasks();
        Files.writeString(dataFolder.resolve("biome-profiles.yml"), """
            profiles:
              minecraft:forest:
                climate:
                  humidity: not-a-number
                  temperature: 0.5
                fertility: 0.5
            """);

        CommandResult reload = server.executeConsole("dynamicbiomes", "reload");

        assertEquals(
            "DynamicBiomes reload failed. See the server log for details.",
            reload.getSender().nextMessage()
        );
        assertEquals(activeListeners, listenersFor(plugin));
        assertEquals(activeTasks, seasonAdvancementTasks());
        server.getScheduler().performTicks(5);
        assertEquals("Current season: minecraft:summer", currentSeason());
    }

    @Test
    void misspelledCropGrowthOverridesRejectReloadAndKeepTheActiveListeners() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        Path cropGrowth = plugin.getDataFolder().toPath().resolve("crop-growth.yml");
        String configuration = """
            crops:
              wheat:
                growth-chance: 1.0
            biomes:
              minecraft:forest:
                wheat:
                  growth-chance: 0.0
            """;
        Files.writeString(cropGrowth, configuration);
        assertEquals(
            "DynamicBiomes configuration reloaded.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        List<Listener> activeListeners = listenersFor(plugin);

        Files.writeString(cropGrowth, configuration.replace("biomes:", "biomse:"));

        assertEquals(
            "DynamicBiomes reload failed. See the server log for details.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        assertEquals(activeListeners, listenersFor(plugin));
    }

    @Test
    void validatesCalendarAndSeasonProfilesTogetherOnReload() throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);
        Path dataFolder = plugin.getDataFolder().toPath();
        Files.writeString(dataFolder.resolve("season-cycle.yml"), """
            calendar: [minecraft:spring, test:monsoon]
            advancement:
              enabled: false
              initial-delay-ticks: 5
              interval-ticks: 7
            """);

        assertEquals(
            "DynamicBiomes reload failed. See the server log for details.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        assertEquals("Current season: minecraft:spring", currentSeason());

        Path profiles = dataFolder.resolve("season-profiles.yml");
        Files.writeString(profiles, Files.readString(profiles) + """
              test:monsoon:
                climate-adjustment:
                  temperature: 0.1
                  humidity: 0.2
            """);
        assertEquals(
            "DynamicBiomes configuration reloaded.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        assertEquals(
            "Current season: test:monsoon",
            server.executeConsole("dynamicbiomes", "season", "set", "test:monsoon").getSender().nextMessage()
        );
        assertTrue(Files.readString(dataFolder.resolve("season-state.yml")).contains("test:monsoon"));

        Files.writeString(dataFolder.resolve("season-cycle.yml"), """
            calendar: [minecraft:spring, minecraft:summer]
            advancement:
              enabled: false
              initial-delay-ticks: 5
              interval-ticks: 7
            """);
        assertEquals(
            "DynamicBiomes reload failed. See the server log for details.",
            server.executeConsole("dynamicbiomes", "reload").getSender().nextMessage()
        );
        assertEquals("Current season: test:monsoon", currentSeason());
    }

    @Test
    void registersTheBiomeCommandAsPlayerOnly() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        assertNotNull(server.getPluginCommand("dynamicbiomes"));

        CommandResult result = server.executeConsole("dynamicbiomes", "biome");

        assertTrue(result.hasSucceeded());
        assertEquals(
            "This command can only be used by a player.",
            result.getSender().nextMessage()
        );
    }

    @Test
    void registersTheInspectCommandAsPlayerOnly() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        assertNotNull(server.getPluginCommand("dynamicbiomes"));

        CommandResult result = server.executeConsole("dynamicbiomes", "inspect");

        assertTrue(result.hasSucceeded());
        assertEquals(
            "This command can only be used by a player.",
            result.getSender().nextMessage()
        );
    }

    @Test
    void withDefaultSeasonCyclePluginSchedulesNoSeasonAdvancementTask() {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        server.getPluginManager().enablePlugin(plugin);

        List<ScheduledTask> advancementTasks = scheduledTasks().stream()
            .filter(task -> SEASON_ADVANCEMENT_TASK_CLASS_NAME.equals(
                task.getRunnable().getClass().getName()
            ))
            .toList();

        assertTrue(
            advancementTasks.isEmpty(),
            "Expected no season advancement tasks with default disabled season-cycle.yml"
        );
    }

    @Test
    void withEnabledSeasonCyclePluginSchedulesExactlyOneRepeatingSeasonAdvancementTask()
        throws IOException {
        Plugin plugin = MockBukkit.loadJar(System.getProperty("dynamicBiomes.pluginJar"));
        Path dataFolder = plugin.getDataFolder().toPath();
        Files.createDirectories(dataFolder);
        Files.writeString(
            dataFolder.resolve("season-cycle.yml"),
            """
            advancement:
              enabled: true
              initial-delay-ticks: 5
              interval-ticks: 7
            """
        );

        server.getPluginManager().enablePlugin(plugin);

        List<RepeatingTask> advancementTasks = scheduledTasks().stream()
            .filter(RepeatingTask.class::isInstance)
            .map(RepeatingTask.class::cast)
            .filter(task -> SEASON_ADVANCEMENT_TASK_CLASS_NAME.equals(
                task.getRunnable().getClass().getName()
            ))
            .toList();

        assertEquals(
            1,
            advancementTasks.size(),
            "Expected exactly one repeating season advancement task"
        );

        RepeatingTask task = advancementTasks.getFirst();
        BukkitSchedulerMock scheduler = (BukkitSchedulerMock) server.getScheduler();
        long expectedFirstRunTick = scheduler.getCurrentTick() + 5L;

        assertEquals(
            expectedFirstRunTick,
            task.getScheduledTick(),
            "Expected first run tick to match configured initial delay"
        );
        assertEquals(
            7L,
            task.getPeriod(),
            "Expected task period to match configured interval"
        );
    }

    private List<ScheduledTask> scheduledTasks() {
        BukkitSchedulerMock scheduler = (BukkitSchedulerMock) server.getScheduler();
        try {
            Field scheduledTasksField = BukkitSchedulerMock.class.getDeclaredField("scheduledTasks");
            scheduledTasksField.setAccessible(true);
            Object taskList = scheduledTasksField.get(scheduler);
            Method getCurrentTaskList = taskList.getClass().getDeclaredMethod("getCurrentTaskList");
            getCurrentTaskList.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ScheduledTask> tasks = (List<ScheduledTask>) getCurrentTaskList.invoke(taskList);
            return tasks;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(
                "Unable to inspect MockBukkit scheduler task list",
                exception
            );
        }
    }

    private String currentSeason() {
        return server.executeConsole("dynamicbiomes", "season").getSender().nextMessage();
    }

    private List<RepeatingTask> seasonAdvancementTasks() {
        return scheduledTasks().stream()
            .filter(RepeatingTask.class::isInstance)
            .map(RepeatingTask.class::cast)
            .filter(task -> SEASON_ADVANCEMENT_TASK_CLASS_NAME.equals(task.getRunnable().getClass().getName()))
            .toList();
    }

    private static List<Listener> listenersFor(Plugin plugin) {
        return HandlerList.getRegisteredListeners(plugin).stream()
            .map(registeredListener -> registeredListener.getListener())
            .distinct()
            .toList();
    }

    private static final class MessageRecordingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}
