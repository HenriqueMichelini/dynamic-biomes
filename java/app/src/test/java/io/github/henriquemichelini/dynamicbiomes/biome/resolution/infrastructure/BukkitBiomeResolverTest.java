package io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure.YamlBiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BukkitBiomeResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void missingBiomeProfileIsReportedAsUnsupportedBiome() throws IOException {
        Path profileFile = writeProfiles(
            """
            profiles:
              minecraft:forest:
                climate:
                  humidity: 0.50
                  temperature: 0.50
                fertility: 0.50
            """
        );
        YamlBiomeProfileProvider profileProvider = new YamlBiomeProfileProvider(profileFile);
        BukkitBiomeResolver resolver = new BukkitBiomeResolver(
            fakeServer(),
            profileProvider
        );

        UnsupportedBiomeException exception = assertThrows(
            UnsupportedBiomeException.class,
            () -> resolver.resolveContext(new BiomeId("minecraft:dripstone_caves"))
        );

        assertTrue(exception.getMessage().contains("minecraft:dripstone_caves"));
    }

    private static Server fakeServer() {
        return (Server) Proxy.newProxyInstance(
            BukkitBiomeResolverTest.class.getClassLoader(),
            new Class<?>[] { Server.class },
            (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private Path writeProfiles(String yaml) throws IOException {
        Path profileFile = temporaryDirectory.resolve("biome-profiles.yml");
        Files.writeString(profileFile, yaml);
        return profileFile;
    }
}
