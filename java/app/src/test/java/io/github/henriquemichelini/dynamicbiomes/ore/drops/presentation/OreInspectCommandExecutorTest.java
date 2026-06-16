package io.github.henriquemichelini.dynamicbiomes.ore.drops.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.henriquemichelini.dynamicbiomes.biome.identity.domain.BiomeId;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.BiomeProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.ClimateProfile;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.EcologicalPressure;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Fertility;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Humidity;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.MineralRichness;
import io.github.henriquemichelini.dynamicbiomes.biome.profile.domain.Temperature;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeContext;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.BiomeResolver;
import io.github.henriquemichelini.dynamicbiomes.biome.resolution.domain.UnsupportedBiomeException;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropOreRule;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicy;
import io.github.henriquemichelini.dynamicbiomes.ore.drops.domain.OreDropPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.ore.identity.domain.OreKind;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.application.OreOriginTrackingService;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOrigin;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginRepository;
import io.github.henriquemichelini.dynamicbiomes.ore.origin.domain.OreOriginType;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class OreInspectCommandExecutorTest {
    private static final UUID WORLD_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000002"
    );
    private static final BlockPosition TARGET_POSITION = new BlockPosition(
        new WorldReference(WORLD_ID),
        12,
        64,
        -8
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final OreKind IRON_ORE = new OreKind("minecraft:iron_ore");
    private static final OreKind DEEPSLATE_DIAMOND_ORE = new OreKind(
        "minecraft:deepslate_diamond_ore"
    );

    @Test
    void reportsSupportedNaturalOreDiagnostics() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.IRON_ORE);
        InMemoryOreOriginRepository originRepository =
            new InMemoryOreOriginRepository();
        OreInspectCommandExecutor command = command(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(biomeId, IRON_ORE),
            originRepository
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Target block: IRON_ORE",
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Ore drop policy: supported",
                "Ore origin: natural/untracked",
                "Eligible for multiplier: yes"
            ),
            sender.messages
        );
        assertTrue(originRepository.savedOrigins.isEmpty());
        assertTrue(originRepository.removedPositions.isEmpty());
    }

    @Test
    void reportsPolicySupportForConfiguredNonIronOreDiagnostics() {
        RecordingSender sender = RecordingSender.playerLookingAt(
            Material.DEEPSLATE_DIAMOND_ORE
        );
        InMemoryOreOriginRepository originRepository =
            new InMemoryOreOriginRepository();
        OreInspectCommandExecutor command = command(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(biomeId, DEEPSLATE_DIAMOND_ORE),
            originRepository
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Target block: DEEPSLATE_DIAMOND_ORE",
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Ore drop policy: supported",
                "Ore origin: natural/untracked",
                "Eligible for multiplier: yes"
            ),
            sender.messages
        );
        assertTrue(originRepository.savedOrigins.isEmpty());
        assertTrue(originRepository.removedPositions.isEmpty());
    }

    @Test
    void reportsPlayerPlacedOreAsIneligible() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.IRON_ORE);
        InMemoryOreOriginRepository originRepository =
            new InMemoryOreOriginRepository();
        originRepository.save(
            new OreOrigin(TARGET_POSITION, OreOriginType.PLAYER_PLACED)
        );
        originRepository.clearMutations();
        OreInspectCommandExecutor command = command(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(biomeId, IRON_ORE),
            originRepository
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Target block: IRON_ORE",
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Ore drop policy: supported",
                "Ore origin: player-placed",
                "Eligible for multiplier: no"
            ),
            sender.messages
        );
        assertTrue(originRepository.savedOrigins.isEmpty());
        assertTrue(originRepository.removedPositions.isEmpty());
    }

    @Test
    void reportsUnsupportedBiomeWithoutCheckingPolicy() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.IRON_ORE);
        CountingOreDropPolicyProvider policyProvider =
            new CountingOreDropPolicyProvider();
        InMemoryOreOriginRepository originRepository =
            new InMemoryOreOriginRepository();
        BiomeId biomeId = new BiomeId("minecraft:dripstone_caves");
        OreInspectCommandExecutor command = command(
            position -> {
                throw new UnsupportedBiomeException(
                    biomeId,
                    "Missing static biome profile for resolved biome: " + biomeId.value()
                );
            },
            policyProvider,
            originRepository
        );

        boolean handled = command.onCommand(
            sender.commandSender(),
            null,
            "dynamicbiomes",
            new String[] { "inspect" }
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Target block: IRON_ORE",
                "Current biome: minecraft:dripstone_caves",
                "DynamicBiomes profile: unsupported",
                "Ore drop policy: skipped",
                "Ore origin: not checked",
                "Eligible for multiplier: no"
            ),
            sender.messages
        );
        assertEquals(0, policyProvider.readCount);
        assertTrue(originRepository.savedOrigins.isEmpty());
        assertTrue(originRepository.removedPositions.isEmpty());
    }

    @Test
    void reportsNonOreTargetWithoutResolvingBiome() {
        RecordingSender sender = RecordingSender.playerLookingAt(Material.STONE);
        CountingBiomeResolver biomeResolver = new CountingBiomeResolver();
        CountingOreDropPolicyProvider policyProvider =
            new CountingOreDropPolicyProvider();
        OreInspectCommandExecutor command = command(
            biomeResolver,
            policyProvider,
            new InMemoryOreOriginRepository()
        );

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
        assertEquals(0, biomeResolver.resolveCount);
        assertEquals(0, policyProvider.readCount);
    }

    @Test
    void rejectsConsoleSendersWithoutInspecting() {
        RecordingSender sender = RecordingSender.console();
        CountingBiomeResolver biomeResolver = new CountingBiomeResolver();
        CountingOreDropPolicyProvider policyProvider =
            new CountingOreDropPolicyProvider();
        OreInspectCommandExecutor command = command(
            biomeResolver,
            policyProvider,
            new InMemoryOreOriginRepository()
        );

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
        assertEquals(0, biomeResolver.resolveCount);
        assertEquals(0, policyProvider.readCount);
    }

    private static OreInspectCommandExecutor command(
        BiomeResolver biomeResolver,
        OreDropPolicyProvider policyProvider,
        InMemoryOreOriginRepository originRepository
    ) {
        return new OreInspectCommandExecutor(
            biomeResolver,
            policyProvider,
            new OreOriginTrackingService(originRepository)
        );
    }

    private static OreDropPolicy policyFor(BiomeId biomeId, OreKind oreKind) {
        return new OreDropPolicy(
            biomeId,
            Map.of(
                oreKind,
                new OreDropOreRule(
                    new OreDropMultiplierRange(1.0, 2.0),
                    Map.of()
                )
            )
        );
    }

    private static BiomeProfile profileFor(BiomeId biomeId) {
        return new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(0.5), new Temperature(0.5)),
            new Fertility(0.5),
            new MineralRichness(0.5),
            new EcologicalPressure(0.5)
        );
    }

    private static final class CountingBiomeResolver implements BiomeResolver {
        private int resolveCount;

        @Override
        public BiomeContext resolve(BlockPosition position) {
            resolveCount++;
            throw new UnsupportedOperationException();
        }
    }

    private static final class CountingOreDropPolicyProvider
        implements OreDropPolicyProvider
    {
        private int readCount;

        @Override
        public OreDropPolicy policyFor(BiomeId biomeId) {
            readCount++;
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryOreOriginRepository
        implements OreOriginRepository
    {
        private final Map<BlockPosition, OreOrigin> origins = new HashMap<>();
        private final List<OreOrigin> savedOrigins = new ArrayList<>();
        private final List<BlockPosition> removedPositions = new ArrayList<>();

        @Override
        public void save(OreOrigin origin) {
            savedOrigins.add(origin);
            origins.put(origin.position(), origin);
        }

        @Override
        public Optional<OreOrigin> findByPosition(BlockPosition position) {
            return Optional.ofNullable(origins.get(position));
        }

        @Override
        public void removeByPosition(BlockPosition position) {
            removedPositions.add(position);
            origins.remove(position);
        }

        void clearMutations() {
            savedOrigins.clear();
            removedPositions.clear();
        }
    }

    private static final class RecordingSender {
        private final List<String> messages = new ArrayList<>();
        private CommandSender commandSender;

        private RecordingSender() {}

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
            World world = world();
            return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[] { Block.class },
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getType" -> material;
                    case "getWorld" -> world;
                    case "getX" -> TARGET_POSITION.x();
                    case "getY" -> TARGET_POSITION.y();
                    case "getZ" -> TARGET_POSITION.z();
                    default -> defaultValue(method.getReturnType());
                }
            );
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
