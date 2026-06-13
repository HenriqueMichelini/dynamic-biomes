package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class DynamicBiomesTest {
    @Test
    void remainsAPaperPluginEntrypoint() {
        assertTrue(JavaPlugin.class.isAssignableFrom(DynamicBiomes.class));
    }

    @Test
    void declaresLifecycleHooksForComposition() {
        assertTrue(
            Arrays.stream(DynamicBiomes.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("onEnable")),
            "Expected onEnable to be declared for startup composition"
        );
        assertTrue(
            Arrays.stream(DynamicBiomes.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("onDisable")),
            "Expected onDisable to be declared for shutdown composition"
        );
    }
}
