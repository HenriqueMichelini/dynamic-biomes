package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

final class DynamicBiomesCommandExecutor implements CommandExecutor {
    private static final String DIAGNOSTICS_PERMISSION = "dynamicbiomes.admin.diagnostics";
    private static final String RELOAD_PERMISSION = "dynamicbiomes.admin.reload";

    private final CommandExecutor seasonCommandExecutor;
    private final CommandExecutor biomeCommandExecutor;
    private final CommandExecutor inspectCommandExecutor;
    private final CommandExecutor diagnosticsCommandExecutor;
    private final BooleanSupplier reload;

    DynamicBiomesCommandExecutor(
        CommandExecutor seasonCommandExecutor,
        CommandExecutor biomeCommandExecutor,
        CommandExecutor inspectCommandExecutor,
        CommandExecutor diagnosticsCommandExecutor,
        BooleanSupplier reload
    ) {
        this.seasonCommandExecutor = Objects.requireNonNull(seasonCommandExecutor);
        this.biomeCommandExecutor = Objects.requireNonNull(biomeCommandExecutor);
        this.inspectCommandExecutor = Objects.requireNonNull(inspectCommandExecutor);
        this.diagnosticsCommandExecutor = Objects.requireNonNull(diagnosticsCommandExecutor);
        this.reload = Objects.requireNonNull(reload);
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
        if (args.length > 0 && args[0].equalsIgnoreCase("diagnostics")) {
            if (!sender.hasPermission(DIAGNOSTICS_PERMISSION)) {
                sender.sendMessage("You do not have permission to view DynamicBiomes diagnostics.");
            } else if (args.length != 1) {
                sender.sendMessage("Usage: /dynamicbiomes diagnostics");
            } else {
                return diagnosticsCommandExecutor.onCommand(sender, command, label, args);
            }
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                sender.sendMessage("You do not have permission to reload DynamicBiomes.");
            } else if (args.length != 1) {
                sender.sendMessage("Usage: /dynamicbiomes reload");
            } else if (reload.getAsBoolean()) {
                sender.sendMessage("DynamicBiomes configuration reloaded.");
            } else {
                sender.sendMessage("DynamicBiomes reload failed. See the server log for details.");
            }
            return true;
        }

        String diagnosticsUsage = sender.hasPermission(DIAGNOSTICS_PERMISSION) ? "|diagnostics" : "";
        String reloadUsage = sender.hasPermission(RELOAD_PERMISSION) ? "|reload" : "";
        sender.sendMessage("Usage: /dynamicbiomes <season|biome|inspect" + diagnosticsUsage + reloadUsage + ">");
        return true;
    }
}
