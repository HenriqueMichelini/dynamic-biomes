package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

final class DynamicBiomesCommandExecutor implements CommandExecutor {
    private static final String USAGE =
        "Usage: /dynamicbiomes <season|biome|inspect>";

    private final CommandExecutor seasonCommandExecutor;
    private final CommandExecutor biomeCommandExecutor;
    private final CommandExecutor inspectCommandExecutor;

    DynamicBiomesCommandExecutor(
        CommandExecutor seasonCommandExecutor,
        CommandExecutor biomeCommandExecutor,
        CommandExecutor inspectCommandExecutor
    ) {
        this.seasonCommandExecutor = Objects.requireNonNull(seasonCommandExecutor);
        this.biomeCommandExecutor = Objects.requireNonNull(biomeCommandExecutor);
        this.inspectCommandExecutor = Objects.requireNonNull(inspectCommandExecutor);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (args.length > 0 && args[0].equalsIgnoreCase("season")) {
            return seasonCommandExecutor.onCommand(sender, command, label, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("biome")) {
            return biomeCommandExecutor.onCommand(sender, command, label, args);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("inspect")) {
            return inspectCommandExecutor.onCommand(sender, command, label, args);
        }

        sender.sendMessage(USAGE);
        return true;
    }
}
