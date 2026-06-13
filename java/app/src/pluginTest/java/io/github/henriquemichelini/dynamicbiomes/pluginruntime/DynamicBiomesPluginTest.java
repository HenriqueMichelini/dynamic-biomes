package io.github.henriquemichelini.dynamicbiomes.pluginruntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.jar.JarFile;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicBiomesPluginTest {
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
