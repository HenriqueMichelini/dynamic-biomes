package io.github.henriquemichelini.dynamicbiomes.seasons.cycle.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
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

        CommandSender commandSender() {
            return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] { CommandSender.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) {
                        recordMessages(arguments);
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
