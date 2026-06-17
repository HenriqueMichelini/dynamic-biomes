package io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedWheatGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.BlockPosition;
import io.github.henriquemichelini.dynamicbiomes.spatial.domain.WorldReference;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class WheatGrowthInspectDiagnosticTest {
    private static final UUID WORLD_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000003"
    );
    private static final BlockPosition TARGET_POSITION = new BlockPosition(
        new WorldReference(WORLD_ID),
        4,
        70,
        9
    );
    private static final BiomeId FOREST = new BiomeId("minecraft:forest");
    private static final SeasonId SPRING = new SeasonId("minecraft:spring");
    private static final SeasonId SUMMER = new SeasonId("minecraft:summer");
    private static final SeasonId WINTER = new SeasonId("minecraft:winter");

    @Test
    void reportsSupportedWheatPolicyWithCurrentSeasonFactorWithoutRolling() {
        RecordingSender sender = new RecordingSender();
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(
                0.5,
                Map.of(WINTER, new WheatGrowthSeasonalFactor(0.5))
            ),
            new RecordingCurrentSeasonQuery(WINTER)
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Wheat growth policy: supported",
                "Configured wheat growth chance: 0.5",
                "Current season: minecraft:winter",
                "Seasonal wheat growth factor: 0.5",
                "Effective wheat growth chance: 0.25",
                "May cancel natural growth: yes"
            ),
            sender.messages
        );
    }

    @Test
    void reportsCappedSeasonalChanceAsNotCancellingNaturalGrowth() {
        RecordingSender sender = new RecordingSender();
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(
                0.75,
                Map.of(SPRING, new WheatGrowthSeasonalFactor(2.0))
            ),
            new RecordingCurrentSeasonQuery(SPRING)
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Wheat growth policy: supported",
                "Configured wheat growth chance: 0.75",
                "Current season: minecraft:spring",
                "Seasonal wheat growth factor: 2.0",
                "Effective wheat growth chance: 1.0",
                "May cancel natural growth: no"
            ),
            sender.messages
        );
    }

    @Test
    void reportsDefaultSeasonalFactorWhenCurrentSeasonHasNoFactor() {
        RecordingSender sender = new RecordingSender();
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(
                0.75,
                Map.of(SUMMER, new WheatGrowthSeasonalFactor(0.5))
            ),
            new RecordingCurrentSeasonQuery(WINTER)
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Wheat growth policy: supported",
                "Configured wheat growth chance: 0.75",
                "Current season: minecraft:winter",
                "Seasonal wheat growth factor: 1.0 (default)",
                "Effective wheat growth chance: 0.75",
                "May cancel natural growth: yes"
            ),
            sender.messages
        );
    }

    @Test
    void reportsUnsupportedBiomeAsVanillaFallbackWithoutReadingPolicy() {
        RecordingSender sender = new RecordingSender();
        CountingWheatGrowthChancePolicyProvider policyProvider =
            new CountingWheatGrowthChancePolicyProvider();
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(WINTER);
        BiomeId biomeId = new BiomeId("minecraft:dripstone_caves");
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> {
                throw new UnsupportedBiomeException(
                    biomeId,
                    "Missing static biome profile for resolved biome: " + biomeId.value()
                );
            },
            policyProvider,
            currentSeasonQuery
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:dripstone_caves",
                "DynamicBiomes profile: unsupported",
                "Wheat growth policy: unsupported",
                "May cancel natural growth: no (vanilla fallback)"
            ),
            sender.messages
        );
        assertEquals(0, policyProvider.readCount);
        assertEquals(0, currentSeasonQuery.queryCount);
    }

    @Test
    void reportsUnsupportedPolicyAsVanillaFallback() {
        RecordingSender sender = new RecordingSender();
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(WINTER);
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> {
                throw new UnsupportedWheatGrowthPolicyException(
                    "Missing wheat growth policy for biome: " + biomeId.value()
                );
            },
            currentSeasonQuery
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Wheat growth policy: unsupported",
                "May cancel natural growth: no (vanilla fallback)"
            ),
            sender.messages
        );
        assertEquals(0, currentSeasonQuery.queryCount);
    }

    @Test
    void propagatesCurrentSeasonQueryFailure() {
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            biomeId -> policyFor(0.75),
            () -> {
                throw new IllegalStateException("Season query failure");
            }
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> diagnostic.inspect(
                new RecordingSender().commandSender(),
                block(Material.WHEAT)
            )
        );

        assertEquals("Season query failure", exception.getMessage());
    }

    @Test
    void ignoresNonWheatTargetsWithoutResolvingBiome() {
        RecordingSender sender = new RecordingSender();
        CountingBiomeResolver biomeResolver = new CountingBiomeResolver();
        CountingWheatGrowthChancePolicyProvider policyProvider =
            new CountingWheatGrowthChancePolicyProvider();
        WheatGrowthInspectDiagnostic diagnostic = diagnostic(
            biomeResolver,
            policyProvider,
            new RecordingCurrentSeasonQuery(WINTER)
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.STONE)
        );

        assertFalse(handled);
        assertTrue(sender.messages.isEmpty());
        assertEquals(0, biomeResolver.resolveCount);
        assertEquals(0, policyProvider.readCount);
    }

    private static WheatGrowthInspectDiagnostic diagnostic(
        BiomeResolver biomeResolver,
        WheatGrowthChancePolicyProvider policyProvider
    ) {
        return diagnostic(
            biomeResolver,
            policyProvider,
            new RecordingCurrentSeasonQuery(WINTER)
        );
    }

    private static WheatGrowthInspectDiagnostic diagnostic(
        BiomeResolver biomeResolver,
        WheatGrowthChancePolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery
    ) {
        return new WheatGrowthInspectDiagnostic(
            biomeResolver,
            policyProvider,
            currentSeasonQuery
        );
    }

    private static WheatGrowthChancePolicy policyFor(double chance) {
        return policyFor(chance, Map.of());
    }

    private static WheatGrowthChancePolicy policyFor(
        double chance,
        Map<SeasonId, WheatGrowthSeasonalFactor> seasonalFactors
    ) {
        return new WheatGrowthChancePolicy(
            new WheatGrowthChance(chance),
            seasonalFactors,
            () -> {
                throw new AssertionError("Diagnostics must not roll growth chance");
            }
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

    private static Block block(Material material) {
        World world = (World) Proxy.newProxyInstance(
            World.class.getClassLoader(),
            new Class<?>[] { World.class },
            (proxy, method, arguments) -> {
                if (method.getName().equals("getUID")) {
                    return WORLD_ID;
                }
                return defaultValue(method.getReturnType());
            }
        );
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
        return null;
    }

    private static final class CountingBiomeResolver implements BiomeResolver {
        private int resolveCount;

        @Override
        public BiomeContext resolve(BlockPosition position) {
            resolveCount++;
            throw new UnsupportedOperationException();
        }
    }

    private static final class CountingWheatGrowthChancePolicyProvider
        implements WheatGrowthChancePolicyProvider
    {
        private int readCount;

        @Override
        public WheatGrowthChancePolicy policyFor(BiomeId biomeId) {
            readCount++;
            throw new UnsupportedOperationException();
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
    }

    private static final class RecordingCurrentSeasonQuery
        implements CurrentSeasonQuery {

        private final SeasonId currentSeason;
        private int queryCount;

        private RecordingCurrentSeasonQuery(SeasonId currentSeason) {
            this.currentSeason = currentSeason;
        }

        @Override
        public SeasonId currentSeason() {
            queryCount++;
            return currentSeason;
        }
    }
}
