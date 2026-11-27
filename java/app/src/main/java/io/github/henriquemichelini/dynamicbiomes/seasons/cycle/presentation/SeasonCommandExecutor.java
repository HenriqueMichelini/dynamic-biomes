package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class SeasonCommandExecutor implements CommandExecutor {
    private static final String USAGE = "Usage: /dynamicbiomes season";
    private static final String SET_USAGE = "Usage: /dynamicbiomes season [set <namespaced-id>]";
    private static final String SET_PERMISSION = "dynamicbiomes.admin.season";

    private final CurrentSeasonQuery currentSeasonQuery;
    private final Consumer<SeasonId> updateSeason;

    public SeasonCommandExecutor(CurrentSeasonQuery currentSeasonQuery) {
        this(currentSeasonQuery, null);
    }

    public SeasonCommandExecutor(
        CurrentSeasonQuery currentSeasonQuery,
        Consumer<SeasonId> updateSeason
    ) {
        this.currentSeasonQuery = Objects.requireNonNull(currentSeasonQuery);
        this.updateSeason = updateSeason;
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

        if (updateSeason != null && args.length == 3 && args[0].equalsIgnoreCase("season")
            && args[1].equalsIgnoreCase("set")) {
            setSeason(sender, args[2]);
            return true;
        }

        sender.sendMessage(updateSeason == null ? USAGE : SET_USAGE);
        return true;
    }

    private void setSeason(CommandSender sender, String requestedSeason) {
        if (!sender.hasPermission(SET_PERMISSION)) {
            sender.sendMessage("You do not have permission to set the season.");
            return;
        }

        try {
            SeasonId season = new SeasonId(requestedSeason);
            updateSeason.accept(season);
            sender.sendMessage("Current season: " + season.value());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(exception.getMessage());
        } catch (IllegalStateException exception) {
            sender.sendMessage("Unable to set the current season. " + exception.getMessage());
        }
    }
}
