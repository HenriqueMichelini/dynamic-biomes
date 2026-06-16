package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class DynamicBiomesCommandExecutorTest {
    @Test
    void routesSeasonSubcommandToSeasonExecutor() {
        RecordingSender sender = new RecordingSender();
        RecordingCommandExecutor seasonExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor biomeExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor inspectExecutor = new RecordingCommandExecutor();
        DynamicBiomesCommandExecutor command = new DynamicBiomesCommandExecutor(
            seasonExecutor,
            biomeExecutor,
            inspectExecutor
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "season" }
        );

        assertTrue(handled);
        assertEquals(1, seasonExecutor.callCount);
        assertEquals(0, biomeExecutor.callCount);
        assertEquals(0, inspectExecutor.callCount);
        assertEquals(List.of("season"), List.of(seasonExecutor.args));
    }

    @Test
    void routesBiomeSubcommandToBiomeExecutor() {
        RecordingSender sender = new RecordingSender();
        RecordingCommandExecutor seasonExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor biomeExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor inspectExecutor = new RecordingCommandExecutor();
        DynamicBiomesCommandExecutor command = new DynamicBiomesCommandExecutor(
            seasonExecutor,
            biomeExecutor,
            inspectExecutor
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "biome" }
        );

        assertTrue(handled);
        assertEquals(0, seasonExecutor.callCount);
        assertEquals(1, biomeExecutor.callCount);
        assertEquals(0, inspectExecutor.callCount);
        assertEquals(List.of("biome"), List.of(biomeExecutor.args));
    }

    @Test
    void routesInspectSubcommandToInspectExecutor() {
        RecordingSender sender = new RecordingSender();
        RecordingCommandExecutor seasonExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor biomeExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor inspectExecutor = new RecordingCommandExecutor();
        DynamicBiomesCommandExecutor command = new DynamicBiomesCommandExecutor(
            seasonExecutor,
            biomeExecutor,
            inspectExecutor
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(0, seasonExecutor.callCount);
        assertEquals(0, biomeExecutor.callCount);
        assertEquals(1, inspectExecutor.callCount);
        assertEquals(List.of("inspect"), List.of(inspectExecutor.args));
    }

    @Test
    void sendsUsageForUnknownSubcommands() {
        RecordingSender sender = new RecordingSender();
        RecordingCommandExecutor seasonExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor biomeExecutor = new RecordingCommandExecutor();
        RecordingCommandExecutor inspectExecutor = new RecordingCommandExecutor();
        DynamicBiomesCommandExecutor command = new DynamicBiomesCommandExecutor(
            seasonExecutor,
            biomeExecutor,
            inspectExecutor
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "reload" }
        );

        assertTrue(handled);
        assertEquals(
            List.of("Usage: /dynamicbiomes <season|biome|inspect>"),
            sender.messages
        );
        assertEquals(0, seasonExecutor.callCount);
        assertEquals(0, biomeExecutor.callCount);
        assertEquals(0, inspectExecutor.callCount);
    }

    private static final class RecordingCommandExecutor implements CommandExecutor {
        private int callCount;
        private String[] args;

        @Override
        public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
        ) {
            callCount++;
            this.args = args;
            return true;
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
