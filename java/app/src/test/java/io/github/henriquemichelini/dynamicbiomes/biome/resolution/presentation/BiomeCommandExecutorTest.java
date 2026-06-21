package io.github.henriquemichelini.dynamicbiomes.biome.resolution.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class BiomeCommandExecutorTest {
    private static final UUID WORLD_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000001"
    );

    @Test
    void reportsSupportedProfileForAPlayerCurrentBiome() {
        RecordingSender sender = RecordingSender.playerAt(10.0, 64.0, -3.0);
        RecordingBiomeResolver resolver = new RecordingBiomeResolver(
            position -> new BiomeContext(
                new BiomeId("minecraft:forest"),
                profileFor(new BiomeId("minecraft:forest"))
            )
        );
        BiomeCommandExecutor command = new BiomeCommandExecutor(resolver);

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "biome" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported"
            ),
            sender.messages
        );
        assertEquals(new BlockPosition(sender.worldReference(), 10, 64, -3), resolver.position);
    }

    @Test
    void reportsUnsupportedProfileWhenResolverRejectsTheCurrentBiome() {
        RecordingSender sender = RecordingSender.playerAt(0.0, -12.0, 8.0);
        BiomeId biomeId = new BiomeId("minecraft:dripstone_caves");
        RecordingBiomeResolver resolver = new RecordingBiomeResolver(
            position -> {
                throw new UnsupportedBiomeException(
                    biomeId,
                    "Missing static biome profile for resolved biome: " + biomeId.value()
                );
            }
        );
        BiomeCommandExecutor command = new BiomeCommandExecutor(resolver);

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "biome" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:dripstone_caves",
                "DynamicBiomes profile: unsupported"
            ),
            sender.messages
        );
        assertEquals(new BlockPosition(sender.worldReference(), 0, -12, 8), resolver.position);
    }

    @Test
    void rejectsConsoleSendersWithoutResolvingBiome() {
        RecordingSender sender = RecordingSender.console();
        CountingBiomeResolver resolver = new CountingBiomeResolver();
        BiomeCommandExecutor command = new BiomeCommandExecutor(resolver);

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "biome" }
        );

        assertTrue(handled);
        assertEquals(
            List.of("This command can only be used by a player."),
            sender.messages
        );
        assertEquals(0, resolver.resolveCount);
    }

    private static BiomeProfile profileFor(BiomeId biomeId) {
        return new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5)
        );
    }

    private interface Resolution {
        BiomeContext resolve(BlockPosition position);
    }

    private static final class RecordingBiomeResolver implements BiomeResolver {
        private final Resolution resolution;
        private BlockPosition position;

        private RecordingBiomeResolver(Resolution resolution) {
            this.resolution = resolution;
        }

        @Override
        public BiomeContext resolve(BlockPosition position) {
            this.position = position;
            return resolution.resolve(position);
        }
    }

    private static final class CountingBiomeResolver implements BiomeResolver {
        private int resolveCount;

        @Override
        public BiomeContext resolve(BlockPosition position) {
            resolveCount++;
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingSender {
        private final List<String> messages = new ArrayList<>();
        private CommandSender commandSender;
        private World world;

        private RecordingSender() {}

        private static RecordingSender playerAt(double x, double y, double z) {
            RecordingSender sender = new RecordingSender();
            sender.world = world();
            Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] { Player.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("sendMessage")) {
                        sender.recordMessages(arguments);
                    }
                    if (method.getName().equals("getLocation")) {
                        return new Location(sender.world, x, y, z);
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

        WorldReference worldReference() {
            return new WorldReference(world.getUID());
        }

        void recordMessages(Object[] arguments) {
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

        private static World world() {
            return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] { World.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getUID")) {
                        return WORLD_ID;
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
            if (returnType == double.class) {
                return 0.0;
            }
            if (returnType == float.class) {
                return 0.0f;
            }
            if (returnType == long.class) {
                return 0L;
            }
            return null;
        }
    }
}
