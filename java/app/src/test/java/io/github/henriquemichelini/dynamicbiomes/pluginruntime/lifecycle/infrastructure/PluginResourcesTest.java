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
        assertEquals("\"1.21\"", valueFor(pluginMetadata, "api-version"));
    }

    @Test
    void oreDropPolicyUsesItsCapabilityOwnedResourceName() throws IOException {
        String oreDropPolicy = readResource("ore-drops.yml");

        assertTrue(oreDropPolicy.contains("minecraft:forest:"));
        assertTrue(oreDropPolicy.contains("ores:"));
        assertTrue(oreDropPolicy.contains("minecraft:iron_ore:"));
        assertNull(CLASS_LOADER.getResource("biomes.yml"));
    }

    @Test
    void cropYieldPolicyUsesItsCapabilityOwnedResourceName() throws IOException {
        String cropYieldPolicy = readResource("crop-yields.yml");

        assertTrue(cropYieldPolicy.contains("minecraft:forest:"));
        assertTrue(cropYieldPolicy.contains("crops:"));
        assertTrue(cropYieldPolicy.contains("wheat:"));
        assertTrue(cropYieldPolicy.contains("multiplier:"));
        assertTrue(cropYieldPolicy.contains("seasonal-factors:"));
    }

    @Test
    void biomeProfilesUseTheirCapabilityOwnedResourceName() throws IOException {
        String biomeProfiles = readResource("biome-profiles.yml");

        assertTrue(biomeProfiles.contains("profiles:"));
        assertTrue(biomeProfiles.contains("minecraft:forest:"));
        assertNull(CLASS_LOADER.getResource("biomes.yml"));
    }

    @Test
    void seasonProfilesUseTheirCapabilityOwnedResourceName() throws IOException {
        String seasonProfiles = readResource("season-profiles.yml");

        assertTrue(seasonProfiles.contains("profiles:"));
        assertTrue(seasonProfiles.contains("climate-adjustment:"));
        assertTrue(seasonProfiles.contains("minecraft:spring:"));
        assertTrue(seasonProfiles.contains("minecraft:summer:"));
        assertTrue(seasonProfiles.contains("minecraft:autumn:"));
        assertTrue(seasonProfiles.contains("minecraft:winter:"));
        assertNull(CLASS_LOADER.getResource("seasons.yml"));
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
