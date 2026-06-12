package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PluginResourcesTest {
    private static final ClassLoader CLASS_LOADER = PluginResourcesTest.class.getClassLoader();

    @Test
    void pluginMetadataResolvesTheLifecycleEntrypoint() throws IOException, ClassNotFoundException {
        String pluginMetadata = readResource("plugin.yml");
        String mainClassName = valueFor(pluginMetadata, "main");

        assertEquals(DynamicBiomes.class, Class.forName(mainClassName));
        assertTrue(pluginMetadata.contains("name: dynamic-biomes"));
        assertEquals(
            '"' + System.getProperty("dynamicBiomes.pluginVersion") + '"',
            valueFor(pluginMetadata, "version")
        );
        assertTrue(pluginMetadata.contains("api-version: '1.21'"));
    }

    @Test
    void oreDropPolicyUsesItsCapabilityOwnedResourceName() throws IOException {
        String oreDropPolicy = readResource("ore-drops.yml");

        assertTrue(oreDropPolicy.contains("ores:"));
        assertTrue(oreDropPolicy.contains("iron_ore:"));
        assertNull(CLASS_LOADER.getResource("biomes.yml"));
    }

    private static String readResource(String name) throws IOException {
        try (InputStream resource = CLASS_LOADER.getResourceAsStream(name)) {
            assertNotNull(resource, () -> "Missing resource: " + name);
            return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String valueFor(String yaml, String key) {
        String prefix = key + ":";
        return yaml.lines()
            .filter(line -> line.startsWith(prefix))
            .map(line -> line.substring(prefix.length()).trim())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing key: " + key));
    }
}
