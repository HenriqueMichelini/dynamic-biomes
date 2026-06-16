package io.github.henriquemichelini.dynamicbiomes.ore.drops.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.UnsupportedOreDropConfigurationException;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class OreInspectCommandExecutor implements CommandExecutor {
    private static final String PLAYER_ONLY =
        "This command can only be used by a player.";
    private static final String USAGE = "Usage: /dynamicbiomes inspect";
    private static final int TARGET_BLOCK_MAX_DISTANCE = 5;
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final Map<Material, OreKind> INSPECTED_ORES = Map.of(
        Material.IRON_ORE,
        IRON_ORE
    );

    private final BiomeResolver biomeResolver;
    private final OreDropPolicyProvider policyProvider;
    private final OreOriginTrackingService originTracking;

    public OreInspectCommandExecutor(
        BiomeResolver biomeResolver,
        OreDropPolicyProvider policyProvider,
        OreOriginTrackingService originTracking
    ) {
        this.biomeResolver = Objects.requireNonNull(biomeResolver);
        this.policyProvider = Objects.requireNonNull(policyProvider);
        this.originTracking = Objects.requireNonNull(originTracking);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("inspect")) {
            sender.sendMessage(USAGE);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY);
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(TARGET_BLOCK_MAX_DISTANCE);
        if (targetBlock == null) {
            sender.sendMessage("Target block: none");
            sender.sendMessage("Inspection: not an inspected ore");
            return true;
        }

        Material material = targetBlock.getType();
        sender.sendMessage("Target block: " + material.name());
        OreKind oreKind = INSPECTED_ORES.get(material);
        if (oreKind == null) {
            sender.sendMessage("Inspection: not an inspected ore");
            return true;
        }

        BlockPosition position = positionOf(targetBlock);
        BiomeContext biomeContext;
        try {
            biomeContext = biomeResolver.resolve(position);
        } catch (UnsupportedBiomeException exception) {
            String biomeId = exception.biomeId()
                .map(BiomeId::value)
                .orElse("unknown");
            sender.sendMessage("Current biome: " + biomeId);
            sender.sendMessage("DynamicBiomes profile: unsupported");
            sender.sendMessage("Ore drop policy: skipped");
            sender.sendMessage("Ore origin: not checked");
            sender.sendMessage("Eligible for multiplier: no");
            return true;
        }

        sender.sendMessage("Current biome: " + biomeContext.biomeId().value());
        sender.sendMessage("DynamicBiomes profile: supported");

        boolean policySupported = hasPolicyRule(biomeContext.biomeId(), oreKind);
        sender.sendMessage(
            "Ore drop policy: " + (policySupported ? "supported" : "unsupported")
        );

        Optional<OreOrigin> origin = originTracking.originAt(position);
        sender.sendMessage("Ore origin: " + originStatus(origin));
        boolean eligible = policySupported
            && origin
                .map(OreOrigin::isEligibleForBiomeBasedMultiplier)
                .orElse(true);
        sender.sendMessage("Eligible for multiplier: " + yesNo(eligible));
        return true;
    }

    private boolean hasPolicyRule(BiomeId biomeId, OreKind oreKind) {
        try {
            OreDropPolicy policy = policyProvider.policyFor(biomeId);
            policy.multiplierRangeFor(oreKind);
            return true;
        } catch (UnsupportedOreDropConfigurationException exception) {
            return false;
        }
    }

    private static BlockPosition positionOf(Block block) {
        World world = block.getWorld();
        return new BlockPosition(
            new WorldReference(world.getUID()),
            block.getX(),
            block.getY(),
            block.getZ()
        );
    }

    private static String originStatus(Optional<OreOrigin> origin) {
        return origin
            .map(OreInspectCommandExecutor::originStatus)
            .orElse("natural/untracked");
    }

    private static String originStatus(OreOrigin origin) {
        if (origin.type() == OreOriginType.PLAYER_PLACED) {
            return "player-placed";
        }
        return "natural/untracked";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
