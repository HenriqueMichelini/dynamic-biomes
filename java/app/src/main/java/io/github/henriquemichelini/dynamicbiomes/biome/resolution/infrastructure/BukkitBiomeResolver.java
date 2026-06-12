package io.github.henriquemichelini.dynamicbiomes.biome.resolution.infrastructure;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Biome;

public final class BukkitBiomeResolver implements BiomeResolver {
    private final Server server;
    private final BiomeProfileProvider profileProvider;

    public BukkitBiomeResolver(Server server, BiomeProfileProvider profileProvider) {
        this.server = Objects.requireNonNull(server, "Bukkit server must not be null");
        this.profileProvider = Objects.requireNonNull(
            profileProvider,
            "Biome profile provider must not be null"
        );
    }

    @Override
    public BiomeContext resolve(BlockPosition position) {
        Objects.requireNonNull(position, "Block position must not be null");
        
        World world = server.getWorld(position.world().id());
        
        if (world == null) {
            throw new IllegalStateException(
                "Unable to resolve biome because Bukkit world is unavailable: "
                    + position.world().id()
            );
        }

        Biome biome = world.getBiome(position.x(), position.y(), position.z());
        BiomeId biomeId = new BiomeId(biome.getKey().toString());
        BiomeProfile profile = profileProvider.profileFor(biomeId);
        
        if (profile == null) {
            throw new IllegalStateException(
                "Missing static biome profile for resolved biome: " + biomeId.value()
            );
        }
        
        return new BiomeContext(biomeId, profile);
    }
}
