package io.github.henriquemichelini.dynamicbiomes.ore.drops.presentation;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService.SupportedBiome;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.application.OreInspectionService.UnsupportedBiome;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.infrastructure.PaperOreMaterialMapper;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

public final class OreInspectDiagnostic {
    private final OreInspectionService inspectionService;

    public OreInspectDiagnostic(OreInspectionService inspectionService) {
        this.inspectionService = Objects.requireNonNull(inspectionService);
    }

    public boolean inspect(CommandSender sender, Block targetBlock) {
        Optional<OreKind> oreKind = PaperOreMaterialMapper.oreKindFor(targetBlock.getType());
        if (oreKind.isEmpty()) return false;

        BlockPosition position = new BlockPosition(
            new WorldReference(targetBlock.getWorld().getUID()),
            targetBlock.getX(),
            targetBlock.getY(),
            targetBlock.getZ()
        );
        switch (inspectionService.inspect(position, oreKind.orElseThrow())) {
            case SupportedBiome result -> reportSupportedBiome(sender, result);
            case UnsupportedBiome result -> reportUnsupportedBiome(sender, result);
        }
        return true;
    }

    private static void reportSupportedBiome(CommandSender sender, SupportedBiome result) {
        sender.sendMessage("Current biome: " + result.biomeId().value());
        sender.sendMessage("DynamicBiomes profile: supported");
        sender.sendMessage(
            "Ore drop policy: " + (result.policySupported() ? "supported" : "unsupported")
        );
        String origin = switch (result.originType()) {
            case NATURAL -> "natural/untracked";
            case PLAYER_PLACED -> "player-placed";
        };
        sender.sendMessage("Ore origin: " + origin);
        sender.sendMessage("Eligible for multiplier: " + (result.eligibleForMultiplier() ? "yes" : "no"));
    }

    private static void reportUnsupportedBiome(CommandSender sender, UnsupportedBiome result) {
        sender.sendMessage("Current biome: " + result.biomeId().map(BiomeId::value).orElse("unknown"));
        sender.sendMessage("DynamicBiomes profile: unsupported");
        sender.sendMessage("Ore drop policy: skipped");
        sender.sendMessage("Ore origin: not checked");
        sender.sendMessage("Eligible for multiplier: no");
    }
}
