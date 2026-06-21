package io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthChance;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.CropGrowthSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.crops.identity.domain.CropKind;
import io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.UnsupportedCropGrowthPolicyException;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.application.EnvironmentalStateComposer;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropEnvironmentalState;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceCalculator;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfile;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.CropPerformanceProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.performance.domain.NormalizedEnvironmentalValue;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldCropRule;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldMultiplierRange;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicy;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldPolicyProvider;
import io.github.henriquemichelini.dynamicbiomes.crops.yield.domain.CropYieldSeasonalFactor;
import io.github.henriquemichelini.dynamicbiomes.seasons.cycle.domain.CurrentSeasonQuery;
import io.github.henriquemichelini.dynamicbiomes.seasons.identity.domain.SeasonId;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonClimateAdjustment;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfile;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonProfileProvider;
import io.github.henriquemichelini.dynamicbiomes.seasons.profile.domain.SeasonalAdjustment;
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

class CropGrowthInspectDiagnosticTest {

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
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.WHEAT, cropKind);
                return policyFor(
                    0.5,
                    Map.of(WINTER, new CropGrowthSeasonalFactor(0.5))
                );
            },
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
                "Biome humidity: 0.5",
                "Biome temperature: 0.5",
                "Biome soil fertility: 0.5",
                "Season humidity adjustment: 0.0",
                "Season humidity factor: 1.0",
                "Season temperature adjustment: 0.0",
                "Season temperature factor: 1.0",
                "Crop environmental wind speed: actual=0.5, preferred=0.5",
                "Crop environmental rain strength: actual=0.5, preferred=0.5",
                "Crop environmental humidity: actual=0.5, preferred=0.5",
                "Crop environmental temperature: actual=0.5, preferred=0.5",
                "Crop environmental solar incidence: actual=0.5, preferred=0.5",
                "Crop environmental soil fertility: actual=0.5, preferred=0.5",
                "Crop performance overall score: 1.0",
                "Crop growth speed factor: 1.0 (not used by natural growth listener)",
                "Crop growth chance factor: 1.0",
                "Harvest quantity factor: 1.0",
                "Effective wheat growth chance before performance: 0.25",
                "Performance-adjusted wheat growth chance: 0.25",
                "May cancel natural growth: yes",
                "Wheat yield policy: supported",
                "Configured wheat yield multiplier range: 1.0—1.0",
                "Seasonal wheat yield factor: 1.0",
                "Effective wheat yield multiplier range: 1.0—1.0",
                "Runtime wheat yield quantity: vanilla produce quantity * selected multiplier, with probabilistic rounding for fractional results"
            ),
            sender.messages
        );
    }

    @Test
    void reportsSupportedCarrotPolicyThroughGenericCropPath() {
        RecordingSender sender = new RecordingSender();
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.CARROTS, cropKind);
                return policyFor(
                    0.4,
                    Map.of(WINTER, new CropGrowthSeasonalFactor(0.5))
                );
            },
            new RecordingCurrentSeasonQuery(WINTER)
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.CARROTS)
        );

        assertTrue(handled);
        assertEquals(
            List.of(
                "Current biome: minecraft:forest",
                "DynamicBiomes profile: supported",
                "Carrot growth policy: supported",
                "Configured carrot growth chance: 0.4",
                "Current season: minecraft:winter",
                "Seasonal carrot growth factor: 0.5",
                "Biome humidity: 0.5",
                "Biome temperature: 0.5",
                "Biome soil fertility: 0.5",
                "Season humidity adjustment: 0.0",
                "Season humidity factor: 1.0",
                "Season temperature adjustment: 0.0",
                "Season temperature factor: 1.0",
                "Crop environmental wind speed: actual=0.5, preferred=0.5",
                "Crop environmental rain strength: actual=0.5, preferred=0.5",
                "Crop environmental humidity: actual=0.5, preferred=0.5",
                "Crop environmental temperature: actual=0.5, preferred=0.5",
                "Crop environmental solar incidence: actual=0.5, preferred=0.5",
                "Crop environmental soil fertility: actual=0.5, preferred=0.5",
                "Crop performance overall score: 1.0",
                "Crop growth speed factor: 1.0 (not used by natural growth listener)",
                "Crop growth chance factor: 1.0",
                "Harvest quantity factor: 1.0",
                "Effective carrot growth chance before performance: 0.2",
                "Performance-adjusted carrot growth chance: 0.2",
                "May cancel natural growth: yes",
                "Carrot yield policy: supported",
                "Configured carrot yield multiplier range: 1.0—1.0",
                "Seasonal carrot yield factor: 1.0",
                "Effective carrot yield multiplier range: 1.0—1.0",
                "Runtime carrot yield quantity: vanilla produce quantity * selected multiplier, with probabilistic rounding for fractional results"
            ),
            sender.messages
        );
    }

    @Test
    void reportsCappedSeasonalChanceAsNotCancellingNaturalGrowth() {
        RecordingSender sender = new RecordingSender();
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.WHEAT, cropKind);
                return policyFor(
                    0.75,
                    Map.of(SPRING, new CropGrowthSeasonalFactor(2.0))
                );
            },
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
                "Biome humidity: 0.5",
                "Biome temperature: 0.5",
                "Biome soil fertility: 0.5",
                "Season humidity adjustment: 0.0",
                "Season humidity factor: 1.0",
                "Season temperature adjustment: 0.0",
                "Season temperature factor: 1.0",
                "Crop environmental wind speed: actual=0.5, preferred=0.5",
                "Crop environmental rain strength: actual=0.5, preferred=0.5",
                "Crop environmental humidity: actual=0.5, preferred=0.5",
                "Crop environmental temperature: actual=0.5, preferred=0.5",
                "Crop environmental solar incidence: actual=0.5, preferred=0.5",
                "Crop environmental soil fertility: actual=0.5, preferred=0.5",
                "Crop performance overall score: 1.0",
                "Crop growth speed factor: 1.0 (not used by natural growth listener)",
                "Crop growth chance factor: 1.0",
                "Harvest quantity factor: 1.0",
                "Effective wheat growth chance before performance: 1.0",
                "Performance-adjusted wheat growth chance: 1.0",
                "May cancel natural growth: no",
                "Wheat yield policy: supported",
                "Configured wheat yield multiplier range: 1.0—1.0",
                "Seasonal wheat yield factor: 1.0",
                "Effective wheat yield multiplier range: 1.0—1.0",
                "Runtime wheat yield quantity: vanilla produce quantity * selected multiplier, with probabilistic rounding for fractional results"
            ),
            sender.messages
        );
    }

    @Test
    void reportsDefaultSeasonalFactorWhenCurrentSeasonHasNoFactor() {
        RecordingSender sender = new RecordingSender();
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.WHEAT, cropKind);
                return policyFor(
                    0.75,
                    Map.of(SUMMER, new CropGrowthSeasonalFactor(0.5))
                );
            },
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
                "Biome humidity: 0.5",
                "Biome temperature: 0.5",
                "Biome soil fertility: 0.5",
                "Season humidity adjustment: 0.0",
                "Season humidity factor: 1.0",
                "Season temperature adjustment: 0.0",
                "Season temperature factor: 1.0",
                "Crop environmental wind speed: actual=0.5, preferred=0.5",
                "Crop environmental rain strength: actual=0.5, preferred=0.5",
                "Crop environmental humidity: actual=0.5, preferred=0.5",
                "Crop environmental temperature: actual=0.5, preferred=0.5",
                "Crop environmental solar incidence: actual=0.5, preferred=0.5",
                "Crop environmental soil fertility: actual=0.5, preferred=0.5",
                "Crop performance overall score: 1.0",
                "Crop growth speed factor: 1.0 (not used by natural growth listener)",
                "Crop growth chance factor: 1.0",
                "Harvest quantity factor: 1.0",
                "Effective wheat growth chance before performance: 0.75",
                "Performance-adjusted wheat growth chance: 0.75",
                "May cancel natural growth: yes",
                "Wheat yield policy: supported",
                "Configured wheat yield multiplier range: 1.0—1.0",
                "Seasonal wheat yield factor: 1.0",
                "Effective wheat yield multiplier range: 1.0—1.0",
                "Runtime wheat yield quantity: vanilla produce quantity * selected multiplier, with probabilistic rounding for fractional results"
            ),
            sender.messages
        );
    }

    @Test
    void reportsCropPerformanceAndYieldVariablesForSupportedCrop() {
        RecordingSender sender = new RecordingSender();
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(
                FOREST,
                profileFor(FOREST, 0.4, 0.6, 0.8)
            ),
            (biomeId, cropKind) -> policyFor(0.5),
            new RecordingCurrentSeasonQuery(SPRING),
            seasonId -> new SeasonProfile(
                seasonId,
                new SeasonClimateAdjustment(
                    new SeasonalAdjustment(-0.25),
                    new SeasonalAdjustment(0.5)
                )
            ),
            position -> environmentalState(0.5, 0.5, 0.6, 0.45, 0.5, 0.8),
            cropKind -> performanceProfileFor(cropKind, 0.25, 0.75),
            biomeId -> yieldPolicyFor(
                CropKind.WHEAT,
                new CropYieldMultiplierRange(1.25, 1.75),
                Map.of(SPRING, new CropYieldSeasonalFactor(0.8))
            )
        );

        boolean handled = diagnostic.inspect(
            sender.commandSender(),
            block(Material.WHEAT)
        );

        assertTrue(handled);
        assertTrue(
            sender.messages.contains(
                "Season humidity adjustment: 0.5"
            )
        );
        assertTrue(sender.messages.contains("Biome humidity: 0.4"));
        assertTrue(sender.messages.contains("Biome temperature: 0.6"));
        assertTrue(sender.messages.contains("Biome soil fertility: 0.8"));
        assertTrue(
            sender.messages.contains(
                "Crop environmental humidity: actual=0.6, preferred=0.25"
            )
        );
        assertTrue(
            sender.messages.contains(
                "Crop performance overall score: 0.8416666666666667"
            )
        );
        assertTrue(
            sender.messages.contains(
                "Performance-adjusted wheat growth chance: 0.42083333333333334"
            )
        );
        assertTrue(
            sender.messages.contains(
                "Configured wheat yield multiplier range: 1.25—1.75"
            )
        );
        assertTrue(
            sender.messages.contains(
                "Effective wheat yield multiplier range: 0.8416666666666667—1.1783333333333335"
            )
        );
    }

    @Test
    void reportsUnsupportedBiomeAsVanillaFallbackWithoutReadingPolicy() {
        RecordingSender sender = new RecordingSender();
        CountingCropGrowthPolicyProvider policyProvider =
            new CountingCropGrowthPolicyProvider();
        RecordingCurrentSeasonQuery currentSeasonQuery =
            new RecordingCurrentSeasonQuery(WINTER);
        BiomeId biomeId = new BiomeId("minecraft:dripstone_caves");
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> {
                throw new UnsupportedBiomeException(
                    biomeId,
                    "Missing static biome profile for resolved biome: " +
                        biomeId.value()
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
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.WHEAT, cropKind);
                throw new UnsupportedCropGrowthPolicyException(
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
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
            position -> new BiomeContext(FOREST, profileFor(FOREST)),
            (biomeId, cropKind) -> {
                assertEquals(CropKind.WHEAT, cropKind);
                return policyFor(0.75);
            },
            () -> {
                throw new IllegalStateException("Season query failure");
            }
        );

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () ->
                diagnostic.inspect(
                    new RecordingSender().commandSender(),
                    block(Material.WHEAT)
                )
        );

        assertEquals("Season query failure", exception.getMessage());
    }

    @Test
    void ignoresUnsupportedCropTargetsWithoutResolvingBiome() {
        RecordingSender sender = new RecordingSender();
        CountingBiomeResolver biomeResolver = new CountingBiomeResolver();
        CountingCropGrowthPolicyProvider policyProvider =
            new CountingCropGrowthPolicyProvider();
        CropGrowthInspectDiagnostic diagnostic = diagnostic(
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

    private static CropGrowthInspectDiagnostic diagnostic(
        BiomeResolver biomeResolver,
        CropGrowthPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery
    ) {
        return diagnostic(
            biomeResolver,
            policyProvider,
            currentSeasonQuery,
            seasonId -> new SeasonProfile(
                seasonId,
                new SeasonClimateAdjustment(
                    new SeasonalAdjustment(0.0),
                    new SeasonalAdjustment(0.0)
                )
            ),
            position -> environmentalState(0.5, 0.5, 0.5, 0.5, 0.5, 0.5),
            cropKind -> performanceProfileFor(cropKind, 0.5),
            biomeId -> defaultYieldPolicy()
        );
    }

    private static CropGrowthInspectDiagnostic diagnostic(
        BiomeResolver biomeResolver,
        CropGrowthPolicyProvider policyProvider,
        CurrentSeasonQuery currentSeasonQuery,
        SeasonProfileProvider seasonProfileProvider,
        EnvironmentalStateComposer environmentalStateComposer,
        CropPerformanceProfileProvider cropPerformanceProfileProvider,
        CropYieldPolicyProvider cropYieldPolicyProvider
    ) {
        return new CropGrowthInspectDiagnostic(
            biomeResolver,
            policyProvider,
            currentSeasonQuery,
            seasonProfileProvider,
            environmentalStateComposer,
            cropPerformanceProfileProvider,
            new CropPerformanceCalculator(),
            cropYieldPolicyProvider
        );
    }

    private static CropGrowthPolicy policyFor(double chance) {
        return policyFor(chance, Map.of());
    }

    private static CropGrowthPolicy policyFor(
        double chance,
        Map<SeasonId, CropGrowthSeasonalFactor> seasonalFactors
    ) {
        return new CropGrowthPolicy(
            new CropGrowthChance(chance),
            seasonalFactors,
            () -> {
                throw new AssertionError(
                    "Diagnostics must not roll growth chance"
                );
            }
        );
    }

    private static BiomeProfile profileFor(BiomeId biomeId) {
        return profileFor(biomeId, 0.5, 0.5, 0.5);
    }

    private static BiomeProfile profileFor(
        BiomeId biomeId,
        double humidity,
        double temperature,
        double fertility
    ) {
        return new BiomeProfile(
            biomeId,
            new ClimateProfile(new Humidity(humidity), new Temperature(temperature)),
            new Fertility(fertility)
        );
    }

    private static CropPerformanceProfile performanceProfileFor(
        CropKind cropKind,
        double preferredValue
    ) {
        return performanceProfileFor(cropKind, preferredValue, preferredValue);
    }

    private static CropPerformanceProfile performanceProfileFor(
        CropKind cropKind,
        double preferredHumidity,
        double preferredTemperature
    ) {
        return new CropPerformanceProfile(
            cropKind,
            new NormalizedEnvironmentalValue(0.5),
            new NormalizedEnvironmentalValue(0.5),
            new NormalizedEnvironmentalValue(preferredHumidity),
            new NormalizedEnvironmentalValue(preferredTemperature),
            new NormalizedEnvironmentalValue(0.5),
            new NormalizedEnvironmentalValue(0.5)
        );
    }

    private static CropEnvironmentalState environmentalState(
        double windSpeed,
        double rainStrength,
        double humidity,
        double temperature,
        double solarIncidence,
        double soilFertility
    ) {
        return new CropEnvironmentalState(
            new NormalizedEnvironmentalValue(windSpeed),
            new NormalizedEnvironmentalValue(rainStrength),
            new NormalizedEnvironmentalValue(humidity),
            new NormalizedEnvironmentalValue(temperature),
            new NormalizedEnvironmentalValue(solarIncidence),
            new NormalizedEnvironmentalValue(soilFertility)
        );
    }

    private static CropYieldPolicy yieldPolicyFor(
        CropKind cropKind,
        CropYieldMultiplierRange multiplierRange,
        Map<SeasonId, CropYieldSeasonalFactor> seasonalFactors
    ) {
        return new CropYieldPolicy(
            FOREST,
            Map.of(
                cropKind,
                new CropYieldCropRule(multiplierRange, seasonalFactors)
            )
        );
    }

    private static CropYieldPolicy defaultYieldPolicy() {
        CropYieldMultiplierRange neutralRange = new CropYieldMultiplierRange(
            1.0,
            1.0
        );
        return new CropYieldPolicy(
            FOREST,
            Map.of(
                CropKind.WHEAT,
                new CropYieldCropRule(neutralRange, Map.of()),
                CropKind.CARROTS,
                new CropYieldCropRule(neutralRange, Map.of()),
                CropKind.POTATOES,
                new CropYieldCropRule(neutralRange, Map.of()),
                CropKind.BEETROOT,
                new CropYieldCropRule(neutralRange, Map.of())
            )
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
            (proxy, method, arguments) ->
                switch (method.getName()) {
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

    private static final class CountingCropGrowthPolicyProvider
        implements CropGrowthPolicyProvider
    {

        private int readCount;

        @Override
        public CropGrowthPolicy policyFor(BiomeId biomeId, CropKind cropKind) {
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
        implements CurrentSeasonQuery
    {

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
