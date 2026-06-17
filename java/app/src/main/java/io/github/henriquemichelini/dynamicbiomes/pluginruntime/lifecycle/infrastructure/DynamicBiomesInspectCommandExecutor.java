package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class DynamicBiomesInspectCommandExecutor implements CommandExecutor {
    private static final String PLAYER_ONLY =
        "This command can only be used by a player.";
    private static final String USAGE = "Usage: /dynamicbiomes inspect";
    private static final int TARGET_BLOCK_MAX_DISTANCE = 5;

    private final List<BiFunction<CommandSender, Block, Boolean>> diagnostics;

    DynamicBiomesInspectCommandExecutor(
        List<BiFunction<CommandSender, Block, Boolean>> diagnostics
    ) {
        this.diagnostics = List.copyOf(Objects.requireNonNull(diagnostics));
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

        sender.sendMessage("Target block: " + targetBlock.getType().name());
        for (BiFunction<CommandSender, Block, Boolean> diagnostic : diagnostics) {
            if (Boolean.TRUE.equals(diagnostic.apply(sender, targetBlock))) {
                return true;
            }
        }

        sender.sendMessage("Inspection: not an inspected ore");
        return true;
    }
}
