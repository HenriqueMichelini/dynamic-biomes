package io.github.henriquemichelini.dynamicbiomes.pluginruntime.lifecycle.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DynamicBiomesInspectCommandExecutorTest {
    @Test
    void sendsTargetBlockAndStopsAtFirstHandledDiagnostic() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.WHEAT);
        RecordingDiagnostic first = new RecordingDiagnostic(false, "first");
        RecordingDiagnostic second = new RecordingDiagnostic(true, "second");
        RecordingDiagnostic third = new RecordingDiagnostic(true, "third");
        DynamicBiomesInspectCommandExecutor command =
            new DynamicBiomesInspectCommandExecutor(
                List.of(first::inspect, second::inspect, third::inspect)
            );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(List.of("Target block: WHEAT", "second"), sender.messages);
        assertEquals(1, first.callCount);
        assertEquals(1, second.callCount);
        assertEquals(0, third.callCount);
    }

    @Test
    void preservesExistingFallbackForUnhandledTargets() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.STONE);
        DynamicBiomesInspectCommandExecutor command =
            new DynamicBiomesInspectCommandExecutor(List.of((ignored, block) -> false));

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Target block: STONE",
                "Inspection: not an inspected ore"
            ),
            sender.messages
        );
    }

    @Test
    void rejectsConsoleSendersWithoutInspecting() {
        RecordingSender sender = RecordingSender.console();
        RecordingDiagnostic diagnostic = new RecordingDiagnostic(true, "handled");
        DynamicBiomesInspectCommandExecutor command =
            new DynamicBiomesInspectCommandExecutor(List.of(diagnostic::inspect));

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of("This command can only be used by a player."),
            sender.messages
        );
        assertEquals(0, diagnostic.callCount);
    }

    private static final class RecordingDiagnostic {
        private final boolean handled;
        private final String message;
        private int callCount;

        private RecordingDiagnostic(boolean handled, String message) {
            this.handled = handled;
            this.message = message;
        }

        private boolean inspect(CommandSender sender, Block block) {
            callCount++;
            if (handled) {
                sender.sendMessage(message);
            }
            return handled;
        }
    }

    private static final class RecordingSender {
        private final List<String> messages = new ArrayList<>();
        private CommandSender commandSender;

        private static RecordingSender playerLookingAt(Material material) {
            RecordingSender sender = new RecordingSender();
            Block block = block(material);
            Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] { Player.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) {
                        sender.recordMessages(arguments);
                    }
                    if (method.getName().equals("getTargetBlockExact")) {
                        return block;
                    }
                    return defaultValue(method.getReturnType());
                }
            );
            sender.commandSender = player;
            return sender;
        }

        private static RecordingSender console() {
            RecordingSender sender = new RecordingSender();
            sender.commandSender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] { CommandSender.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) {
                        sender.recordMessages(arguments);
                    }
                    return defaultValue(method.getReturnType());
                }
            );
            return sender;
        }

        CommandSender commandSender() {
            return commandSender;
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

        private static Block block(Material material) {
            return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[] { Block.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getType")) {
                        return material;
                    }
                    return defaultValue(method.getReturnType());
                }
            );
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
