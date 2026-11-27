package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonCalendar;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.SeasonStatePersistenceException;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class SeasonCommandExecutorTest {
    @Test
    void sendsTheCurrentSeasonForTheSeasonSubcommand() {
        RecordingSender sender = new RecordingSender();
        CurrentSeasonQuery currentSeasonQuery = () ->
            new SeasonId("minecraft:summer");
        SeasonCommandExecutor command = new SeasonCommandExecutor(
            currentSeasonQuery
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "season" }
        );

        assertTrue(handled);
        assertEquals(List.of("Current season: minecraft:summer"), sender.messages);
    }

    @Test
    void sendsUsageWithoutASubcommand() {
        RecordingSender sender = new RecordingSender();
        SeasonCommandExecutor command = new SeasonCommandExecutor(
            () -> new SeasonId("minecraft:summer")
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[0]
        );

        assertTrue(handled);
        assertEquals(List.of("Usage: /dynamicbiomes season"), sender.messages);
    }

    @Test
    void sendsUsageWithoutReadingTheSeasonForAnUnknownSubcommand() {
        RecordingSender sender = new RecordingSender();
        CountingCurrentSeasonQuery currentSeasonQuery =
            new CountingCurrentSeasonQuery();
        SeasonCommandExecutor command = new SeasonCommandExecutor(
            currentSeasonQuery
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "advance" }
        );

        assertTrue(handled);
        assertEquals(List.of("Usage: /dynamicbiomes season"), sender.messages);
        assertEquals(0, currentSeasonQuery.readCount);
    }

    @Test
    void setsSeasonForAnAuthorizedSender() {
        RecordingSender sender = new RecordingSender(true);
        List<SeasonId> updates = new ArrayList<>();

        assertTrue(setSeason(sender, updates::add, "minecraft:winter"));

        assertEquals(List.of(new SeasonId("minecraft:winter")), updates);
        assertEquals(List.of("Current season: minecraft:winter"), sender.messages);
    }

    @Test
    void deniesSeasonUpdatesWithoutTheAdminPermission() {
        RecordingSender sender = new RecordingSender();
        List<SeasonId> updates = new ArrayList<>();

        assertTrue(setSeason(sender, updates::add, "minecraft:winter"));

        assertTrue(updates.isEmpty());
        assertEquals(List.of("You do not have permission to set the season."), sender.messages);
    }

    @Test
    void reportsAnInvalidSeasonIdWithoutAttemptingAnUpdate() {
        RecordingSender sender = new RecordingSender(true);
        List<SeasonId> updates = new ArrayList<>();

        assertTrue(setSeason(sender, updates::add, "winter"));

        assertTrue(updates.isEmpty());
        assertEquals(
            List.of("Season identity must be a valid lowercase namespaced identifier"),
            sender.messages
        );
    }

    @Test
    void reportsPersistenceFailureWithoutReportingSuccess() {
        RecordingSender sender = new RecordingSender(true);

        assertTrue(setSeason(sender, season -> {
            throw new SeasonStatePersistenceException("Persistence failed", null);
        }, "minecraft:winter"));

        assertEquals(List.of("Unable to set the current season. Persistence failed"), sender.messages);
    }

    @Test
    void reportsASeasonOutsideTheCalendarWithoutReportingSuccess() {
        RecordingSender sender = new RecordingSender(true);

        assertTrue(setSeason(sender, SeasonCalendar.standard()::requireSeason, "custom:monsoon"));

        assertEquals(
            List.of("Season is not in the configured calendar: custom:monsoon"),
            sender.messages
        );
    }

    @Test
    void propagatesUnexpectedFailuresFromTheSeasonUpdate() {
        RecordingSender sender = new RecordingSender(true);

        for (RuntimeException failure : List.of(
            new IllegalArgumentException("Invalid dependency input"),
            new IllegalStateException("Invalid dependency state")
        )) {
            assertSame(failure, assertThrows(RuntimeException.class, () ->
                setSeason(sender, season -> { throw failure; }, "minecraft:winter")
            ));
        }

        assertTrue(sender.messages.isEmpty());
    }

    private static boolean setSeason(
        RecordingSender sender,
        Consumer<SeasonId> updateSeason,
        String requestedSeason
    ) {
        SeasonCommandExecutor command = new SeasonCommandExecutor(
            () -> new SeasonId("minecraft:summer"), updateSeason
        );
        return command.onCommand(
            sender.commandSender(), null, "dynamicbiomes",
            new String[] { "season", "set", requestedSeason }
        );
    }

    private static final class CountingCurrentSeasonQuery
        implements CurrentSeasonQuery {
        private int readCount;

        @Override
        public SeasonId currentSeason() {
            readCount++;
            return new SeasonId("minecraft:summer");
        }
    }

    private static final class RecordingSender {
        private final List<String> messages = new ArrayList<>();
        private final boolean seasonAdmin;

        private RecordingSender() {
            this(false);
        }

        private RecordingSender(boolean seasonAdmin) {
            this.seasonAdmin = seasonAdmin;
        }

        CommandSender commandSender() {
            return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] { CommandSender.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) {
                        recordMessages(arguments);
                    }
                    if (method.getName().equals("hasPermission")) {
                        return seasonAdmin && "dynamicbiomes.admin.season".equals(arguments[0]);
                    }
                    return defaultValue(method.getReturnType());
                }
            );
        }

        private void recordMessages(Object[] arguments) {
            if (arguments == null) {
                return;
            }
            for (Object argument : arguments) {
                if (argument instanceof String message) {
                    messages.add(message);
                } else if (argument instanceof String[] messageArray) {
                    messages.addAll(List.of(messageArray));
                }
            }
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            return null;
        }
    }
}
