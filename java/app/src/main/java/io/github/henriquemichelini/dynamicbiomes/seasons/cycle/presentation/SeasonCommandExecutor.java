package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class SeasonCommandExecutor implements CommandExecutor {
    private static final String USAGE = "Usage: /dynamicbiomes season";

    private final CurrentSeasonQuery currentSeasonQuery;

    public SeasonCommandExecutor(CurrentSeasonQuery currentSeasonQuery) {
        this.currentSeasonQuery = Objects.requireNonNull(currentSeasonQuery);
    }

    @Override
    public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("season")) {
            sender.sendMessage(
                "Current season: " + currentSeasonQuery.currentSeason().value()
            );
            return true;
        }

        sender.sendMessage(USAGE);
        return true;
    }
}
