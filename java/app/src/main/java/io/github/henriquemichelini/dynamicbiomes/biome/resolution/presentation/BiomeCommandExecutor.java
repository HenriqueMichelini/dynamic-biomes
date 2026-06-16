package io.github.henriquemichelini.dynamicbiomes.biome.resolution.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BiomeCommandExecutor implements CommandExecutor {
    private static final String PLAYER_ONLY =
        "This command can only be used by a player.";
    private static final String USAGE = "Usage: /dynamicbiomes biome";

    private final BiomeResolver biomeResolver;

    public BiomeCommandExecutor(BiomeResolver biomeResolver) {
        this.biomeResolver = Objects.requireNonNull(biomeResolver);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("biome")) {
            sender.sendMessage(USAGE);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY);
            return true;
        }

        try {
            BiomeContext context = biomeResolver.resolve(positionOf(player));
            sendStatus(sender, context.biomeId().value(), "supported");
        } catch (UnsupportedBiomeException exception) {
            String biomeId = exception.biomeId()
                .map(BiomeId::value)
                .orElse("unknown");
            sendStatus(sender, biomeId, "unsupported");
        }

        return true;
    }

    private static BlockPosition positionOf(Player player) {
        Location location = player.getLocation();
        World world = Objects.requireNonNull(
            location.getWorld(),
            "Player location must have a world"
        );
        return new BlockPosition(
            new WorldReference(world.getUID()),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    private static void sendStatus(
        CommandSender sender,
        String biomeId,
        String profileStatus
    ) {
        sender.sendMessage("Current biome: " + biomeId);
        sender.sendMessage("DynamicBiomes profile: " + profileStatus);
    }
}
