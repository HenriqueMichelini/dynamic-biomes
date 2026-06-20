# DynamicBiomes — Architecture

## 1. Goal

Improve isolation, cognitive locality, and dependency hygiene across the plugin.
This document is design-only: no feature implementation, no runtime behavior changes.

## 2. Layout Principle

DDD-inspired modular monolith:

```text
root package
└── bounded context
    └── capability
        └── architectural layer
```

Layers are used only when a capability needs them. **Do not create empty layer packages preemptively.**

## 3. Dependency Direction

```text
                 ┌─────────────────────────────────────┐
                 │          COMPOSITION ROOT            │
                 │          pluginruntime               │
                 │ wires startup and root command routes │
                 └──────────────┬──────────────────────┘
                                │ depends on
                                ▼
                 ┌─────────────────────────────────────┐
                 │    DOWNSTREAM FEATURE CONTEXTS       │
                 │    ore, crops, trees, animals        │
                 │ interpret environmental conditions   │
                 └──────────────┬──────────────────────┘
                                │ depend on
                                ▼
                 ┌─────────────────────────────────────┐
                 │   UPSTREAM ENVIRONMENTAL CONTEXTS    │
                 │   biome, seasons                     │
                 │ describe environmental conditions    │
                 └─────────────────────────────────────┘
```

Rules:

- `pluginruntime` may import all modules, but **only for composition/startup**
  and thin root command routing.
- **No module imports `pluginruntime`.**
- **`biome` and `seasons` must not import feature domains.**
- Feature domains may import upstream vocabulary/ports, but **never upstream infrastructure**.
- Upstream contexts do not depend on downstream contexts.

## 4. Published Language

Downstream feature contexts must consume only explicitly published upstream contracts. Feature domains must not import arbitrary internal packages from `biome` or `seasons`.

### 4.1 What Is Published

- **Biome identity and resolution**: `BiomeId`, `BiomeContext`, `BiomeResolver` port, `UnsupportedBiomeException`.
- **Biome static profile**: `BiomeProfile`, `BiomeProfileProvider` port, `ClimateProfile`, `Humidity`, `Temperature`, `Fertility`, `MineralRichness`, `EcologicalPressure`.
- **Season identity and profile**: `SeasonId`, `CurrentSeasonQuery` port, `SeasonProfile`, `SeasonClimateAdjustment`, `SeasonalAdjustment`, `SeasonProfileProvider` port.
- **Ore drops safety contract**: `UnsupportedOreDropConfigurationException` (thrown when a biome has no ore drop policy or an `OreKind` has no configured rule; caught by `ore/drops/application` to preserve vanilla drops).
- **Spatial vocabulary**: `WorldReference`, `BlockPosition`.

These upstream contracts describe environmental conditions and identity only.
`biome`, `seasons`, and future weather capabilities must not publish
crop-specific growth, yield, quality, or performance rule vocabulary.

### 4.2 What Is Not Published

- Internal infrastructure packages: `biome/*/infrastructure`, `seasons/*/infrastructure`.
- Internal repository implementations: `BukkitBiomeResolver`.
- Internal configuration parsing details.
- Any package not listed in the published vocabulary above.

### 4.3 Consumption Rules

- Downstream domains may import from `biome/*/domain` and `seasons/*/domain` only for types explicitly listed above.
- Downstream domains must not depend on how `biome` or `seasons` persist or resolve their data.
- If a downstream domain needs a new upstream concept, it must be explicitly added to the published language rather than reaching into internal packages.

## 5. Layer Responsibilities

### 5.1 `domain/`

- Value objects, entities, and aggregates.
- Domain services that encode business rules and invariants.
- Domain events.
- Repository and provider **ports** (interfaces).
- Business rules and invariants live here.

Rules:

- No Bukkit/Paper imports.
- No YAML, file I/O, or database implementation details.
- No runtime framework annotations.
- Lombok compile-time annotations are allowed only for boilerplate reduction and
  invariant enforcement, such as null checks. Domain behavior and invariants
  must remain explicit in the domain model.

### 5.2 `application/`

- Use-case orchestration only.
- Loads inputs, calls domain behavior, saves results, coordinates ports.
- Transactional boundaries (if any) live here.

Rules:

- Must not contain domain rules or invariants.
- Must not import Bukkit events or listeners.
- May import domain types and port interfaces.

### 5.3 `infrastructure/`

- Bukkit/Paper adapters (listeners, tasks, world access).
- YAML parsing, file I/O, persistence adapters.
- Framework-specific configuration loading.
- Translation between framework types and domain value objects at boundaries.

Rules:

- Implements domain ports; does not define them.
- May import Bukkit, Paper, SnakeYAML, and file I/O libraries.

### 5.4 `presentation/` (optional)

- Commands, admin UI, menus, user-facing adapters.
- Use only when a capability has player-facing interaction.

Rules:

- Do not create empty `presentation/` packages.
- Must not contain domain logic.

## 6. Shared Kernel: Spatial

`spatial` is **not** a full bounded context. It is a Shared Kernel containing pure, reusable spatial vocabulary.

Responsibilities:

- Owns value objects: `WorldReference`, `BlockPosition`, `ChunkPosition`, `EcologicalRegionId`.
- Must own **no gameplay behavior**.
- Must own **no persistence decisions**.
- Must import **no Bukkit types**.
- May use Lombok compile-time annotations under the same restrictions as domain
  packages.

Bukkit `Location` is translated into these value objects at infrastructure boundaries. Other contexts may freely import from `spatial` because it is a shared kernel, not a downstream dependency.

## 7. Anti-Coupling Rules

- `biome` must not contain ore, crop, tree, or animal vocabulary.
- `seasons` must not reference `biome`.
- Feature domains combine `biome` + `seasons` + feature-specific policy at calculation time.
- Dynamic biome/ecological state must describe **environmental conditions**, not feature-specific behavior.

Forbidden inside `biome/dynamics` or `biome/profile`:

```java
// Forbidden
oreMultiplier
cropGrowthSpeed
animalDeathChance
treeSpreadChance
```

Acceptable inside `biome/profile`:

```java
// Acceptable
ClimateProfile
Humidity
Temperature
Fertility
MineralRichness
EcologicalPressure
```

Do **not** introduce `BiomeEffect`, `EffectType`, or any biome-owned rule vocabulary such as `ORE_DROP_MULTIPLIER`.

Configuration files must be split by ownership:

- `biome-profiles.yml` belongs to `biome/profile`
- `season-profiles.yml` belongs to `seasons/profile`
- `ore-drops.yml` belongs to `ore/drops`
- `crop-growth.yml` belongs to `crops/growth`
- `crop-yields.yml` belongs to `crops/yield`
- `crop-profiles.yml` belongs to `crops/performance`

Raw YAML must not leak into domain. Bukkit types must not leak into domain.

Feature-owned configuration may be keyed by published upstream identity types
when the feature owns the policy semantics. For example, `ore-drops.yml` may
contain ore-specific drop policy keyed by `BiomeId`, and `crop-yields.yml` may
contain crop-specific yield policy keyed by `BiomeId`, because those files
belong to `ore/drops` and `crops/yield`. Biome-scoped crop yield policy is
transitional once crop performance exists; new crop environmental interpretation
belongs in `crops/performance` and must use crop-owned profile/preference data.

Feature-specific behavior remains forbidden in upstream environmental profiles.
`biome-profiles.yml` must not contain fields such as `cropYieldMultiplier` or
`oreDropMultiplier`, and `biome/profile` must not own crop-specific or
ore-specific behavior. Feature contexts interpret published environmental
contracts and identities into their own behavior.

`crop-growth.yml` and `crop-yields.yml` remain transitional crop-owned policy
sources for one phase. The target crop-owned environmental preference/profile
source is `crop-profiles.yml` under `crops/performance`.

## 8. Target Structure

```text
io.github.henriquemichelini.dynamicbiomes/
├── spatial/
│   └── domain/
│       ├── WorldReference.java
│       └── BlockPosition.java
│
├── biome/
│   ├── identity/
│   │   └── domain/
│   │       └── BiomeId.java
│   ├── resolution/
│   │   ├── domain/
│   │   │   ├── BiomeResolver.java
│   │   │   ├── BiomeContext.java
│   │   │   └── UnsupportedBiomeException.java
│   │   ├── infrastructure/
│   │   │   └── BukkitBiomeResolver.java
│   │   └── presentation/
│   │       └── BiomeCommandExecutor.java
│   └── profile/
│       ├── domain/
│       │   ├── BiomeProfile.java
│       │   ├── ClimateProfile.java
│       │   ├── BiomeProfileProvider.java
│       │   ├── Humidity.java
│       │   ├── Temperature.java
│       │   ├── Fertility.java
│       │   ├── MineralRichness.java
│       │   └── EcologicalPressure.java
│       └── infrastructure/
│           └── YamlBiomeProfileProvider.java
│
├── seasons/
│   ├── identity/
│   │   └── domain/
│   │       └── SeasonId.java
│   ├── cycle/
│   │   ├── domain/
│   │   │   ├── SeasonCalendar.java
│   │   │   ├── SeasonStateRepository.java
│   │   │   ├── CurrentSeasonQuery.java
│   │   │   └── SeasonCycleSettings.java
│   │   ├── application/
│   │   │   ├── SeasonInitializationService.java
│   │   │   ├── SeasonAdvancementService.java
│   │   │   └── CachedCurrentSeasonQuery.java
│   │   ├── infrastructure/
│   │   │   ├── YamlSeasonStateRepository.java
│   │   │   ├── YamlSeasonCycleSettingsProvider.java
│   │   │   └── SeasonAdvancementTask.java
│   │   └── presentation/
│   │       └── SeasonCommandExecutor.java
│   └── profile/
│       ├── domain/
│       │   ├── SeasonProfile.java
│       │   ├── SeasonClimateAdjustment.java
│       │   ├── SeasonalAdjustment.java
│       │   └── SeasonProfileProvider.java
│       └── infrastructure/
│           └── YamlSeasonProfileProvider.java
│
├── ore/
│   ├── identity/
│   │   ├── domain/
│   │   │   └── OreKind.java
│   │   └── infrastructure/
│   │       └── PaperOreMaterialMapper.java
│   ├── origin/
│   │   ├── domain/
│   │   │   ├── OreOrigin.java
│   │   │   ├── OreOriginRepository.java
│   │   │   └── OreOriginType.java
│   │   ├── application/
│   │   │   └── OreOriginTrackingService.java
│   │   └── infrastructure/
│   │       ├── PaperOrePlaceListener.java
│   │       ├── PaperOreMovementListener.java
│   │       └── YamlOreOriginRepository.java
│   └── drops/
│       ├── domain/
│       │   ├── OreDropMultiplierCalculator.java
│       │   ├── OreDropMultiplierRange.java
│       │   ├── OreDropMultiplierVariationSource.java
│       │   ├── OreDropOreRule.java
│       │   ├── OreDropPolicy.java
│       │   ├── OreDropPolicyProvider.java
│       │   ├── OreDropQuantityCalculator.java
│       │   ├── OreDropQuantityVariationSource.java
│       │   ├── OreDropSeasonalAdjustment.java
│       │   └── UnsupportedOreDropConfigurationException.java
│       ├── application/
│       │   └── OreDropService.java
│       ├── infrastructure/
│       │   ├── PaperOreBreakListener.java
│       │   ├── PaperOreDropDeltaNotifier.java
│       │   ├── PaperVanillaDropScanner.java
│       │   └── YamlOreDropPolicyProvider.java
│       └── presentation/
│           └── OreInspectCommandExecutor.java
│
├── crops/
│   ├── identity/
│   │   ├── domain/
│   │   │   └── CropKind.java
│   │   └── infrastructure/
│   │       └── PaperCropMaterialMapper.java
│   ├── performance/
│   │   ├── application/
│   │   │   ├── CropEnvironmentalStateComposer.java
│   │   │   └── CropPerformanceService.java
│   │   ├── domain/
│   │   │   ├── CropEnvironmentalState.java
│   │   │   ├── CropPerformanceProfile.java
│   │   │   ├── CropPerformanceProfileProvider.java
│   │   │   └── CropPerformanceResult.java
│   │   └── infrastructure/
│   │       └── YamlCropPerformanceProfileProvider.java
│   ├── growth/
│   │   ├── application/
│   │   │   └── CropGrowthService.java
│   │   ├── domain/
│   │   │   ├── UnsupportedCropGrowthPolicyException.java
│   │   │   ├── CropGrowthChance.java
│   │   │   ├── CropGrowthPolicy.java
│   │   │   ├── CropGrowthPolicyProvider.java
│   │   │   ├── CropGrowthSeasonalFactor.java
│   │   │   ├── CropGrowthChanceVariationSource.java
│   │   │   └── CropGrowthDecision.java
│   │   ├── infrastructure/
│   │   │   ├── PaperCropGrowthListener.java
│   │   │   └── YamlCropGrowthPolicyProvider.java
│   │   └── presentation/
│   │       └── CropGrowthInspectDiagnostic.java
│   └── yield/
│       ├── application/
│       │   └── CropYieldService.java
│       ├── domain/
│       │   ├── UnsupportedCropYieldPolicyException.java
│       │   ├── CropYieldCropRule.java
│       │   ├── CropYieldBiomeFactorCalculator.java
│       │   ├── CropYieldClimateFactorCalculator.java
│       │   ├── CropYieldEffectiveMultiplierCalculator.java
│       │   ├── CropYieldEnvironmentalFactor.java
│       │   ├── CropYieldEnvironmentalFactorCalculator.java
│       │   ├── CropYieldMultiplierCalculator.java
│       │   ├── CropYieldMultiplierRange.java
│       │   ├── CropYieldMultiplierVariationSource.java
│       │   ├── CropYieldPolicy.java
│       │   ├── CropYieldPolicyProvider.java
│       │   ├── CropYieldQuantityCalculator.java
│       │   ├── CropYieldQuantityVariationSource.java
│       │   └── CropYieldSeasonalFactor.java
│       └── infrastructure/
│           ├── PaperCropHarvestListener.java
│           └── YamlCropYieldPolicyProvider.java
│
├── trees/
│   └── growth/
│       └── domain/
│           ├── TreeGrowthPolicy.java
│           ├── TreeGrowthChance.java
│           ├── TreeGrowthSeasonalFactor.java
│           ├── TreeGrowthChanceVariationSource.java
│           └── TreeGrowthDecision.java
│
└── pluginruntime/
    └── lifecycle/
        └── infrastructure/
            ├── DynamicBiomes.java
            ├── DynamicBiomesCommandExecutor.java
            └── DynamicBiomesInspectCommandExecutor.java
```

### 8.1 Spatial Value Objects

Replace generic `WorldPosition` with explicit spatial value objects:

- `WorldReference` — pure Java representation of a world; prefer UUID for identity; may carry name for diagnostics.
- `BlockPosition` — world + int x + int y + int z; used for block events and biome resolution.

Additional spatial value objects (`ChunkPosition`, `EcologicalRegionId`) should not be implemented until used. Bukkit `Location` is translated into these VOs at infrastructure boundaries.

### 8.2 Biome Read Model

`BiomeContext` is the single read model for resolved biome/environment information.

It combines:

- `BiomeId` — the identity of the resolved biome.
- `BiomeProfile` — static or configured ecological properties for that biome type.

There is no separate `ResolvedBiome`. `BiomeResolver` returns `BiomeContext`.

Dynamic state must not contain ore, crop, or animal rule results.

### 8.3 Ore Origin Separate from Ore Drops

- `ore/origin` owns origin state and origin persistence (`OreOrigin`, `OreOriginRepository`).
- `PaperOrePlaceListener` lives in `ore/origin/infrastructure`.
- `ore/drops` consumes ore origin data but does not own origin tracking.
- `OreOrigin` uses pure domain types only; no Bukkit imports.

### 8.4 Crop Performance Owns Crop Environmental Interpretation

`crops/performance` is the crop-owned environmental interpretation capability.
It consumes published upstream environmental contracts and turns them into
crop-specific performance factors. Upstream contexts contribute conditions;
they do not know which crops prefer those conditions.

Responsibilities:

- `biome`, `seasons`, and future weather capabilities describe environmental
  contributors such as climate, season adjustments, and weather state.
- `crops/performance` composes a crop-side `CropEnvironmentalState` read model
  from those contributors.
- `CropEnvironmentalState` stores normalized `0.0..1.0` variables for wind
  speed, rain strength, humidity, temperature, solar incidence, and soil
  fertility.
- `crop-profiles.yml` is the crop-owned preference/profile source for crop
  performance.
- Crop performance output may expose crop behavior factors such as growth speed,
  growth chance, and harvest quantity factors.

Forbidden:

- Do not add crop-specific vocabulary to `biome`, `seasons`, or future weather
  contributors.
- Do not add quality tier, quality probability, quality roll, item quality,
  quality diagnostics, or `/dynamicbiomes inspect` quality output.

## 9. Value Object Rules

- **No empty wrappers.** A value object must carry meaningful, validated data.
- **Validate construction invariants.** IDs cannot be null or blank. Namespaced keys must be valid. Coordinate objects must use explicit integer block/chunk/region semantics.
- **Records are appropriate** for immutable value objects (`BiomeId`, `BlockPosition`, `SeasonId`, etc.).
- **Entities/aggregates should not be passive records** unless immutability and all transition methods are explicitly modeled.
- **Equality is by value** for value objects, by identity for entities.

## 11. Persistence and Configuration Are Infrastructure

### 11.1 Persistence

Persistence is infrastructure, not a bounded context. The domain that owns the state owns the repository port.

State repository ports live inside the owning domain/capability:

```text
ore/origin/domain/OreOriginRepository.java
seasons/cycle/domain/SeasonStateRepository.java
```

Persistence adapters implement those ports:

```text
ore/origin/infrastructure/YamlOreOriginRepository.java
seasons/cycle/infrastructure/YamlSeasonStateRepository.java
```

Shared persistence utilities, if needed, are technical support infrastructure only. There is no generic snapshot/repository abstraction; each domain owns its own repository port.
Persistence format is an implementation detail, not a domain boundary.

### 11.2 Configuration

Configuration is infrastructure, not a bounded context. Each feature/environmental context owns its own typed configuration interpretation.

Typed providers per capability:

```text
biome/profile/infrastructure/YamlBiomeProfileProvider.java
ore/drops/infrastructure/YamlOreDropPolicyProvider.java
seasons/profile/infrastructure/YamlSeasonProfileProvider.java
```

Shared YAML loading/parsing utilities, if needed, are infrastructure support only. There is no generic `ConfigurationProvider` port in domain. Raw YAML is translated into typed domain objects at infrastructure boundaries.

## 12. Port-Adapter Naming Convention

Not every port is a repository. Use naming that reflects responsibility:

| Pattern | Use For | Example |
|---|---|---|
| **Repository** | Persisted collections of domain objects or mutable state | `OreOriginRepository` |
| **Provider** | Configured policy/profile data | `OreDropPolicyProvider`, `BiomeProfileProvider`, `SeasonProfileProvider` |
| **Resolver** | Mapping one concept to another | `BiomeResolver` maps `BlockPosition` to `BiomeContext` |

## 13. Domain Events

Domain events are allowed for cross-context reactions.

Appropriate uses:

- `SeasonTransitioned` — downstream domains react to season changes.

Rules:

- Do **not** force event-driven architecture for synchronous Minecraft actions such as block breaking or entity damage.
- Synchronous feature evaluation may still query published upstream ports directly.
- Events must be plain data objects; no Bukkit types.
- Publish from domain; subscribe in application or infrastructure layers.

## 14. Public Plugin API

Avoid a god `DynamicBiomesApi`.

- **Prefer no public API** until a real external plugin integration use case exists.
- If a central API remains under `pluginruntime/api`, it must be **thin**, contain **no domain logic**, and delegate to context-specific capabilities.
- Public API types must be stable, versioned, and part of the published language.
- Public API must not expose internal repository implementations, YAML details, or Bukkit types to consumers.

## 15. Module-Boundary Enforcement

Enforce boundaries in phases:

- **Phase 1 — Package conventions plus documented import rules.**
  - Follow the layer responsibilities in Section 5.
  - No downstream imports of upstream `infrastructure`.
  - No imports of `pluginruntime` from other modules.
  - Document exceptions explicitly.

- **Phase 2 — ArchUnit-style tests.**
  - Add tests that assert forbidden imports (e.g., `domain` must not import `org.bukkit`, downstream must not import upstream `infrastructure`).
  - Run as part of the normal test suite.

- **Phase 3 — Gradle subprojects (only if the project grows).**
  - Consider Gradle subprojects with explicit `api`/`implementation` dependencies if the codebase exceeds ~50k lines or if independent release cycles are needed.
  - Do **not** require Gradle subprojects immediately.

## 16. Test Package Parity

Tests mirror source package structure:

```text
src/main/java/biome/resolution/domain/BiomeContext.java
src/test/java/biome/resolution/domain/BiomeContextTest.java
```

Rules:

- Domain tests must not require Bukkit, YAML, or file I/O.
- Use in-memory fakes/stubs for providers, repositories, resolvers, and queries.
- Example: `InMemoryBiomeProfileProvider`, `InMemoryOreOriginRepository`.
- Infrastructure tests may use Bukkit mocks or temporary files where appropriate.
- Application tests use real domain objects with stubbed ports.

## 17. Presentation Layer Is Optional

Use `presentation/` only when a capability has commands, admin GUI, public UI, or other presentation-specific adapters.

Good:

```text
ore/drops/
├── domain/
├── application/
└── infrastructure/
```

Add `presentation/` only when needed. Do not create empty layer packages preemptively.

## 18. Implemented Runtime and Deferred Work

### 18.1 Implemented Runtime Behavior

The following capabilities are wired in `pluginruntime/lifecycle/infrastructure/DynamicBiomes` and active at runtime:

- **Ore origin tracking**: `PaperOrePlaceListener` records player-placed ore; `PaperOreBreakListener` clears origin on break; `PaperOreMovementListener` transfers tracked origin across piston movement.
- **Ore drop behavior**: `PaperOreBreakListener` delegates to `OreDropService` for supported configured Overworld ore materials resolved through `PaperOreMaterialMapper`, which maps Bukkit `Material` values to domain `OreKind` values. The service resolves the biome, looks up the ore drop policy, applies the base multiplier, and applies an optional ore-owned seasonal multiplier factor from `ore-drops.yml` based on the cached current season. The listener keeps Paper-specific vanilla drop interpretation in `PaperVanillaDropScanner`; when the final quantity differs from the vanilla quantity, it delegates mining-player delta action-bar and sound feedback to `PaperOreDropDeltaNotifier`.
- **YAML-backed configuration**: `YamlBiomeProfileProvider`, `YamlOreDropPolicyProvider`, `YamlCropGrowthPolicyProvider`, `YamlCropYieldPolicyProvider`, `YamlSeasonProfileProvider`, and `YamlSeasonCycleSettingsProvider` load configured profiles, policies, and cycle settings at startup. `season-profiles.yml` is packaged, copied to the plugin data folder, and used by crop yield runtime composition through the published `SeasonProfileProvider` port.
- **Current season initialization**: `SeasonInitializationService` validates any persisted current season against `SeasonCalendar`, initializes the first season if none exists, and `CachedCurrentSeasonQuery` keeps the runtime season in memory for hot-path reads.
- **Configured season advancement**: `DynamicBiomes` reads `season-cycle.yml`; when `advancement.enabled` is true, it schedules a single repeating `SeasonAdvancementTask` that advances the persisted season through `SeasonCalendar`.
- **Crop growth behavior**: `PaperCropMaterialMapper` maps Bukkit crop materials to supported runtime crop kinds, currently wheat, carrots, potatoes, and beetroot. `PaperCropGrowthListener` delegates supported natural crop `BlockGrowEvent` attempts to `CropGrowthService` with the mapped crop kind; the service resolves biome through `BiomeResolver`, reads configured biome-specific crop growth policy through `YamlCropGrowthPolicyProvider` from `crop-growth.yml`, applies any crop-owned seasonal factor for the cached current season, and cancels growth only when the policy returns a cancel decision.
- **Crop harvest yield behavior**: `PaperCropHarvestListener` delegates supported mature player `BlockBreakEvent` crop harvests to `CropYieldService` with the mapped crop kind and the server-computed vanilla produce quantity. The service resolves biome through `BiomeResolver`, reads the configured biome-scoped crop yield policy through `YamlCropYieldPolicyProvider` from `crop-yields.yml`, reads the current season through `CachedCurrentSeasonQuery`, reads season profile data through `YamlSeasonProfileProvider`, applies the selected biome/crop multiplier, any yield-owned seasonal factor, and the season-climate-derived climate yield factor, and returns the adjusted produce quantity. The listener replaces only produce drops when the quantity differs, preserves non-produce drops such as seeds unchanged, and does not handle replanting or non-player crop destruction paths.
- **Read-only observability commands**: `/dynamicbiomes season` reads the cached `CurrentSeasonQuery` and reports the current `SeasonId`; `/dynamicbiomes biome` resolves the player's current `BiomeContext` through `BiomeResolver` and reports whether the biome has a supported DynamicBiomes profile; `/dynamicbiomes inspect` reads the player's target block, delegates to crop and ore diagnostics, reports crop growth policy support/configured chance/current season/seasonal factor/effective chance/fallback status for supported crop targets, checks ore drop policy/rules through `OreDropPolicyProvider`, reads tracked ore origin through `OreOriginTrackingService`, and reports multiplier eligibility without mutating state.
- **Ore origin persistence**: `YamlOreOriginRepository` lazily loads origin state into memory and writes updates back to disk.

### 18.2 Implemented Supporting Behavior

The following capabilities support runtime behavior while keeping ownership boundaries explicit:

- **Crop growth policy**: `crops/growth/domain` models explicit supported crop kinds, an already-selected configured natural crop growth allow chance, optional crop-owned seasonal factors keyed by published `SeasonId`, a deterministic-testable unit variation source, and an allow/cancel decision. It currently supports configured wheat, carrot, potato, and beetroot policies, and does not resolve biomes or current season state, read configuration, listen for Bukkit events, or mutate world state.
- **Biome-aware crop growth service**: `crops/growth/application` resolves the `BiomeContext` for a `BlockPosition` through the published `BiomeResolver`, loads the configured crop growth policy for a supplied `CropKind` through `CropGrowthPolicyProvider`, reads current season through the published `CurrentSeasonQuery`, and delegates season-aware allow/cancel decisions to domain policy. It preserves vanilla growth for explicit unsupported biome or unsupported crop growth policy cases.
- **Paper crop growth listener**: `crops/growth/infrastructure` maps Bukkit crop materials through `PaperCropMaterialMapper`, translates supported `BlockGrowEvent` crop growth attempts into `BlockPosition`, delegates to `CropGrowthService`, and cancels only when the service returns a cancel decision.
- **YAML-backed crop growth policy provider**: `crops/growth/infrastructure` loads `crop-growth.yml` into the typed `CropGrowthPolicyProvider` port for configured biome-specific wheat, carrot, potato, and beetroot growth chances and optional crop-owned seasonal factors while preserving the existing per-crop YAML shape. It rejects unsupported crop keys and does not listen for Bukkit crop events, query current season state, or mutate world state.
- **Crop yield policy**: `crops/yield/domain` models configured mature crop produce multipliers, optional yield-owned seasonal factors keyed by published `SeasonId`, deterministic-testable multiplier and quantity variation sources, and probabilistic rounding of the final produce quantity. It supports zero final produce quantity when configured multipliers allow it and does not model Bukkit item stacks, seeds, replanting, or block events.
- **Crop yield environmental factors**: `crops/yield/domain` currently models transitional crop-yield-owned interpretation of published season climate adjustments through `CropYieldClimateFactorCalculator`. The earlier fertility-derived biome factor calculators remain present domain code for now, but they are not wired into active crop yield runtime while biome-scoped crop yield policy owns biome influence. The target environmental interpretation capability is `crops/performance`.
- **Biome-aware crop yield service**: `crops/yield/application` resolves the `BiomeContext` for a `BlockPosition` through the published `BiomeResolver`, loads configured biome-scoped crop yield policy through `CropYieldPolicyProvider`, reads current season through the published `CurrentSeasonQuery`, and delegates multiplier and quantity calculation to the domain. Its runtime constructor consumes the published `SeasonProfileProvider`, derives the season-climate factor, composes the selected biome/crop multiplier with the crop seasonal factor and climate factor, and delegates final quantity rounding to `CropYieldQuantityCalculator`. The service preserves vanilla produce quantity only for explicit unsupported biome or unsupported crop yield policy cases.
- **YAML-backed crop yield policy provider**: `crops/yield/infrastructure` loads `crop-yields.yml` into the typed `CropYieldPolicyProvider` port for configured biome-scoped crop-specific multiplier ranges for wheat, carrots, potatoes, and beetroot plus optional yield-owned seasonal factors. `CropYieldPolicyProvider.policyFor(BiomeId)` remains biome-aware, and `YamlCropYieldPolicyProvider` uses the supplied biome id to select the configured policy. It rejects unsupported biome keys, unsupported crop keys, and keeps yield factors independent from crop growth factors.
- **Crop growth inspect diagnostic**: `crops/growth/presentation` maps the targeted block through `PaperCropMaterialMapper` and translates supported crop targets into read-only diagnostics by resolving biome support, querying `CropGrowthPolicyProvider` for the configured chance, and reading `CurrentSeasonQuery` to report the current season, seasonal factor/default, effective chance, and vanilla fallback status without rolling a growth decision.
- **Tree growth policy**: `trees/growth/domain` models an already-selected configured natural tree growth allow chance, optional tree-owned seasonal factors keyed by published `SeasonId`, a deterministic-testable unit variation source, and an allow/cancel decision. It does not resolve biomes or current season state, read configuration, listen for Bukkit events, or mutate world state.

### 18.3 Implemented Safety Behavior

- **Silk Touch bypass**: `PaperOreBreakListener` detects when the broken ore would drop itself (Silk Touch) and skips `OreDropService`, preserving vanilla drops.
- **Unsupported configuration fallback**: `OreDropService` catches `UnsupportedBiomeException` and `UnsupportedOreDropConfigurationException` and returns the vanilla quantity, so unsupported biomes, missing policies, and missing ore rules do not suppress vanilla drops.
- **Crop yield unsupported fallback**: `CropYieldService` catches `UnsupportedBiomeException` and `UnsupportedCropYieldPolicyException` and returns the vanilla produce quantity, so unsupported biomes, missing yield policies, and missing crop rules preserve vanilla harvest drops.
- **Real failures still propagate**: malformed YAML, I/O failures, invalid numeric values, invalid namespaced IDs, duplicate keys, and programming errors are not swallowed by the fallback.
- **Persisted season validation**: `SeasonInitializationService` throws `IllegalStateException` if a persisted `SeasonId` is absent from the configured `SeasonCalendar`.
- **Origin cache safety**: `YamlOreOriginRepository` loads once and serves subsequent reads from memory; save/remove still persist.

### 18.4 Crop Performance Target Semantics and Still-Deferred Work

Current crop yield runtime still uses biome-scoped crop yield policy owned by
`crops/yield`. That shape is valid as transitional feature-owned policy because
`crop-yields.yml` belongs to `crops/yield`, not `biome/profile`, but it is not
the target crop environmental interpretation model.

The target model is:

- `biome`, `seasons`, and future weather capabilities remain upstream
  environmental contributors. They describe environmental conditions and must
  not gain crop-specific vocabulary.
- `crops/performance` owns crop-specific environmental interpretation.
- `CropEnvironmentalState` is a crop-side read model with normalized `0.0..1.0`
  values for wind speed, rain strength, humidity, temperature, solar incidence,
  and soil fertility.
- `crop-profiles.yml` is the crop-owned source for crop environmental
  preferences/profiles.
- Crop performance produces behavior factors for crop systems, not quality
  output.

During the transitional phase:

- `crop-growth.yml` remains the active crop growth policy source.
- `crop-yields.yml` remains the active crop yield policy source.
- Biome-scoped crop yield multipliers remain supported but are transitional
  once crop performance exists.
- The active runtime crop yield multiplier formula remains:

  ```text
  effectiveMultiplier =
    selectedBiomeCropMultiplier
    * cropSeasonalFactor
    * climateYieldFactor
  ```

- `selectedBiomeCropMultiplier` is a `crops/yield` configured multiplier range
  selected by resolved `BiomeId` and `CropKind`. `cropSeasonalFactor` remains
  crop-specific seasonal tuning from `crop-yields.yml`. `climateYieldFactor` is
  the current `crops/yield` interpretation of published `SeasonProfile` climate
  adjustment data and is transitional once `crops/performance` owns crop
  environmental interpretation.
- The current active `crop-yields.yml` shape is biome-scoped and crop-specific.
  Like `ore-drops.yml`, the root mapping is keyed directly by published
  `BiomeId` values:

  ```yaml
  minecraft:forest:
    crops:
      wheat:
        multiplier:
          min: 1.00
          max: 1.10
        seasonal-factors:
          minecraft:spring: 1.05
          minecraft:summer: 1.00
          minecraft:autumn: 1.00
          minecraft:winter: 0.90

      carrots:
        multiplier:
          min: 0.95
          max: 1.05
        seasonal-factors:
          minecraft:spring: 1.00
          minecraft:summer: 1.05
          minecraft:autumn: 1.00
          minecraft:winter: 0.90

  minecraft:desert:
    crops:
      wheat:
        multiplier:
          min: 0.70
          max: 0.90
        seasonal-factors:
          minecraft:spring: 1.00
          minecraft:summer: 0.90
          minecraft:autumn: 1.00
          minecraft:winter: 1.00
  ```

- The previous base-range-only `crop-yields.yml` shape is superseded and is no
  longer the active runtime shape.
- `CropYieldPolicyProvider` must remain biome-aware while crop yield policy is
  biome-scoped. Cleanup that removes `BiomeId` from
  `CropYieldPolicyProvider.policyFor(BiomeId)` is deferred until crop
  performance replaces biome-scoped crop yield policy in a later card.
- `BiomeProfile` fertility remains valid environmental data. However,
  fertility-derived `biomeYieldFactor` must not be applied on top of
  `selectedBiomeCropMultiplier` by default, because both represent biome
  influence. When biome-scoped crop multipliers are active, the
  fertility-derived factor should be removed from active crop yield runtime,
  neutralized by default, or deferred into `crops/performance`.
- Once crop performance exists, crop growth and crop yield may consume crop
  performance factors instead of interpreting upstream environmental conditions
  themselves. For example:

  ```text
  effectiveMultiplier =
    selectedBiomeCropMultiplier
    * cropPerformanceHarvestQuantityFactor
    * cropSeasonalFactor
    * climateYieldFactor
  ```

  That migration requires later cards and must not push crop vocabulary into
  `biome/profile`, `seasons/profile`, or future weather contributors.
- Current pluginruntime crop yield composition uses the corrected biome-scoped
  crop yield service constructor in `DynamicBiomes`. The current composition
  formula is:

  ```text
  adjustedQuantity =
    probabilisticRound(
      vanillaProduceQuantity
      * selectedBiomeCropMultiplier
      * cropSeasonalFactor
      * climateYieldFactor
    )
  ```
- Crop-yield ownership stays split by context:
  crop-specific yield policy belongs to `crops/yield`, biome profile data
  belongs to `biome/profile`, and season profile data belongs to
  `seasons/profile`. `biome` and `seasons` must remain environmental contexts
  and must not gain crop-specific rule vocabulary. Downstream crop code may
  consume published biome and season domain contracts, but not upstream
  infrastructure.
- Quality tier, quality probability, quality roll, item quality, quality
  diagnostics, and `/dynamicbiomes inspect` quality output are out of scope for
  crop performance.
- Runtime tree growth behavior and broader season effects on animals beyond the currently modeled feature policies. Season profile data is modeled and has a YAML provider; crop yield runtime consumes it, while other feature runtimes do not yet consume season profile data.
- Ecological region state and dynamic biome state.
- Admin commands, public API, or configuration reload commands.
- Database persistence.
- Broader gameplay balancing beyond the current ore drop multiplier.

## 19. Preserved Principles

- `biome` is not a god/master domain.
- `biome` owns biome identity, resolution, profile, and plugin-owned ecological state.
- `seasons` is separate and orthogonal to `biome`.
- Feature domains such as `ore`, `crops`, `trees`, and `animals` interpret environmental context into behavior.
- Domain packages must not import Bukkit/Paper APIs.
- Bukkit, YAML, file I/O, database, and plugin lifecycle code belong outside domain.
