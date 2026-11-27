package io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState;
import io.github.henriquemichelini.dynamicbiomes.biome.dynamics.domain.EcologicalRegionState.SkyWeather;
import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Optional;
import lombok.NonNull;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;

public final class BukkitBiomeResolver implements BiomeResolver {

    private final Server server;
    private final BiomeProfileProvider profileProvider;

    public BukkitBiomeResolver(
        @NonNull Server server,
        @NonNull BiomeProfileProvider profileProvider
    ) {
        this.server = server;
        this.profileProvider = profileProvider;
    }

    @Override
    public BiomeContext resolve(@NonNull BlockPosition position) {
        World world = server.getWorld(position.world().id());

        if (world == null) {
            throw new IllegalStateException(
                "Unable to resolve biome because Bukkit world is unavailable: " +
                    position.world().id()
            );
        }

        Biome biome = world.getBiome(position.x(), position.y(), position.z());
        BiomeId biomeId = new BiomeId(biome.getKey().toString());
        return resolveContext(biomeId, world, position);
    }

    BiomeContext resolveContext(BiomeId biomeId, World world, BlockPosition position) {
        BiomeProfile profile = profileProvider.profileFor(biomeId);

        if (profile == null) {
            throw new UnsupportedBiomeException(
                biomeId,
                "Missing static biome profile for resolved biome: " +
                    biomeId.value()
            );
        }

        if (world.getEnvironment() != World.Environment.NORMAL) {
            return new BiomeContext(biomeId, profile);
        }

        EcologicalRegionState ecologicalState = EcologicalRegionState.observedAt(
            position, skyWeather(world), world.getTime()
        );
        return new BiomeContext(biomeId, profile, Optional.of(ecologicalState));
    }

    private static SkyWeather skyWeather(World world) {
        if (!world.hasStorm()) {
            return SkyWeather.CLEAR;
        }
        return world.isThundering() ? SkyWeather.THUNDER : SkyWeather.STORM;
    }
}
