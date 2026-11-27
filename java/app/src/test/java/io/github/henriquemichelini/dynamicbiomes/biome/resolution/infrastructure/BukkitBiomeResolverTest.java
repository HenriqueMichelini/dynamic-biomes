package io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState;
import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState.DayPhase;
import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState.SkyWeather;
import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.infrastructure.YamlBiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.EcologicalRegionId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BukkitBiomeResolverTest {
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final BiomeProfile FOREST_PROFILE = new BiomeProfile(
        FOREST,
        new ClimateProfile(new Humidity(0.50), new Temperature(0.50)),
        new Fertility(0.50)
    );
    private static final BlockPosition POSITION = new BlockPosition(
        new WorldReference(new UUID(0, 1)), -128, 64, 128
    );

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
            () -> resolver.resolveContext(
                new BiomeId("minecraft:dripstone_caves"), null, POSITION
            )
        );

        assertTrue(exception.getMessage().contains("minecraft:dripstone_caves"));
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, 6000, CLEAR, DAY",
        "true, false, 12000, STORM, SUNSET",
        "true, true, 14000, THUNDER, NIGHT"
    })
    void capturesCurrentOverworldConditionsUsingOnlyWorldTimeAndWeather(
        boolean storm,
        boolean thunder,
        long time,
        SkyWeather expectedWeather,
        DayPhase expectedDayPhase
    ) {
        BukkitBiomeResolver resolver = new BukkitBiomeResolver(fakeServer(), id -> FOREST_PROFILE);
        World world = fakeWorld(World.Environment.NORMAL, storm, thunder, () -> time);

        BiomeContext context = resolver.resolveContext(FOREST, world, POSITION);

        assertEquals(
            Optional.of(
                new EcologicalRegionState(
                    new EcologicalRegionId(POSITION.world(), -1, 1),
                    expectedWeather,
                    expectedDayPhase
                )
            ),
            context.ecologicalState()
        );
        assertEquals(FOREST_PROFILE, context.profile());
    }

    @Test
    void observesClockChangesOnTheNextResolutionInTheSameRegion() {
        BukkitBiomeResolver resolver = new BukkitBiomeResolver(fakeServer(), id -> FOREST_PROFILE);
        AtomicLong clock = new AtomicLong(6000);
        World world = fakeWorld(World.Environment.NORMAL, false, false, clock::get);

        EcologicalRegionState day = resolver
            .resolveContext(FOREST, world, POSITION)
            .ecologicalState()
            .orElseThrow();
        clock.set(14000);
        EcologicalRegionState night = resolver
            .resolveContext(FOREST, world, POSITION)
            .ecologicalState()
            .orElseThrow();

        assertEquals(day.regionId(), night.regionId());
        assertEquals(DayPhase.DAY, day.dayPhase());
        assertEquals(DayPhase.NIGHT, night.dayPhase());
    }

    @Test
    void doesNotReadSkyConditionsInNonOverworldDimensions() {
        BukkitBiomeResolver resolver = new BukkitBiomeResolver(fakeServer(), id -> FOREST_PROFILE);
        World world = fakeWorld(World.Environment.NETHER, false, false, () -> 0);

        BiomeContext context = resolver.resolveContext(FOREST, world, POSITION);

        assertEquals(Optional.empty(), context.ecologicalState());
        assertEquals(FOREST_PROFILE, context.profile());
    }

    private static World fakeWorld(
        World.Environment environment,
        boolean storm,
        boolean thunder,
        LongSupplier clock
    ) {
        return (World) Proxy.newProxyInstance(
            World.class.getClassLoader(),
            new Class<?>[] { World.class },
            (proxy, method, args) -> {
                if (method.getName().equals("getEnvironment")) {
                    return environment;
                }
                if (environment == World.Environment.NORMAL) {
                    return switch (method.getName()) {
                        case "hasStorm" -> storm;
                        case "isThundering" -> thunder;
                        case "getTime" -> clock.getAsLong();
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
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
