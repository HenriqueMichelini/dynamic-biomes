package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class DynamicBiomesTest {
    @Test
    void remainsAPaperPluginEntrypoint() {
        assertTrue(JavaPlugin.class.isAssignableFrom(DynamicBiomes.class));
    }
}
