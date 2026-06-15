package io.github.henriquemichelini.dynamicbiomes.pluginruntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.jar.JarFile;
import org.bukkit.event.HandlerList;
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
            assertNotNull(pluginJar.getEntry("biome-profiles.yml"));
            assertNotNull(pluginJar.getEntry("season-profiles.yml"));
            assertNotNull(pluginJar.getEntry("season-cycle.yml"));
            assertNull(pluginJar.getEntry("biomes.yml"));
            assertEquals(System.getProperty("dynamicBiomes.pluginVersion"), plugin.getPluginMeta().getVersion());
            assertEquals(
                "io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure.DynamicBiomes",
                plugin.getPluginMeta().getMainClass()
            );
        }
    }

    @Test
    void registersOreOriginPlaceAndBreakListenersOnEnable() {
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
