package io.github.henriquemichelini.dynamicbiomes.ore.origin.presentation;

import io.github.henriquemichelini.dynamicbiomes.ore.origin.infrastructure.YamlOreOriginRepository.Diagnostics;
import java.util.Locale;
import java.util.function.Supplier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class OreOriginDiagnosticsCommandExecutor implements CommandExecutor {
    private final Supplier<Diagnostics> diagnostics;

    public OreOriginDiagnosticsCommandExecutor(Supplier<Diagnostics> diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Diagnostics snapshot = diagnostics.get();
        sender.sendMessage(String.format(
            Locale.ROOT,
            "Ore-origin write measurement window: %.3f s since plugin startup",
            snapshot.window().toNanos() / 1_000_000_000.0
        ));
        sender.sendMessage(String.format(
            Locale.ROOT,
            "Ore-origin writes: %d attempts, %d succeeded, %d failed",
            snapshot.writeAttempts(),
            snapshot.writeSuccesses(),
            snapshot.writeFailures()
        ));
        String p95 = snapshot.p95UpperBound()
            .map(duration -> String.format(
                Locale.ROOT, "<= %.3f ms", Math.ceil(duration.toNanos() / 1_000.0) / 1_000.0
            ))
            .orElse("no data (no write attempts)");
        sender.sendMessage("Approximate p95 write duration: " + p95);
        sender.sendMessage("Tracked ore origins: " + (snapshot.trackedOrigins().isPresent()
            ? Integer.toString(snapshot.trackedOrigins().getAsInt())
            : "not loaded"));
        return true;
    }
}
