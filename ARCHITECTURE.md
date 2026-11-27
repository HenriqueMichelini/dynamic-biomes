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
- **Biome static profile**: `BiomeProfile`, `BiomeProfileProvider` port, `ClimateProfile`, `Humidity`, `Temperature`, `Fertility`.
- **Biome ecological observations**: `EcologicalRegionState`, `SkyWeather`, and `DayPhase`.
- **Season identity and profile**: `SeasonId`, `CurrentSeasonQuery` port, `SeasonProfile`, `SeasonClimateAdjustment`, `SeasonalAdjustment`, `SeasonProfileProvider` port.
- **Ore drops safety contract**: `UnsupportedOreDropConfigurationException` (thrown when a biome has no ore drop policy or an `OreKind` has no configured rule; caught by `ore/drops/application` to preserve vanilla drops).
- **Spatial vocabulary**: `WorldReference`, `BlockPosition`, `EcologicalRegionId`.

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
```

Do **not** introduce `BiomeEffect`, `EffectType`, or any biome-owned rule vocabulary such as `ORE_DROP_MULTIPLIER`.

Configuration files must be split by ownership:

- `biome-profiles.yml` belongs to `biome/profile`
- `season-profiles.yml` belongs to `seasons/profile`
- `ore-drops.yml` belongs to `ore/drops`
- `crop-growth.yml` belongs to `crops/growth`
- `crop-yields.yml` belongs to `crops/yield`
- `crop-profiles.yml` belongs to `crops/performance`
- `season-cycle.yml` belongs to `seasons/cycle`
- `tree-growth.yml` belongs to `trees/growth`
- `animal-breeding.yml` belongs to `animals/breeding`

Raw YAML must not leak into domain. Bukkit types must not leak into domain.

Feature-owned configuration may be keyed by published upstream identity types
when the feature owns the policy semantics. For example, `ore-drops.yml` may
contain ore-specific drop policy keyed by `BiomeId`, and `crop-yields.yml` may
contain crop-specific yield policy keyed by `BiomeId`, because those files
belong to `ore/drops` and `crops/yield`. Existing biome-specific crop rules
can override crop-wide defaults. Crop environmental interpretation belongs in
`crops/performance` and uses crop-owned profile and preference data.

Feature-specific behavior remains forbidden in upstream environmental profiles.
`biome-profiles.yml` must not contain fields such as `cropYieldMultiplier` or
`oreDropMultiplier`, and `biome/profile` must not own crop-specific or
ore-specific behavior. Feature contexts interpret published environmental
contracts and identities into their own behavior.

`crop-growth.yml` and `crop-yields.yml` own base gameplay tuning and optional
season factors. `crop-profiles.yml` owns environmental preferences under
`crops/performance`.

## 8. Target Structure

```text
io.github.henriquemichelini.dynamicbiomes/
├── spatial/
│   └── domain/
│       ├── WorldReference.java
│       ├── BlockPosition.java
│       └── EcologicalRegionId.java
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
│   ├── dynamics/
│   │   └── domain/EcologicalRegionState.java
│   └── profile/
│       ├── domain/
│       │   ├── BiomeProfile.java
│       │   ├── ClimateProfile.java
│       │   ├── BiomeProfileProvider.java
│       │   ├── Humidity.java
│       │   ├── Temperature.java
│       │   └── Fertility.java
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
│       │   ├── OreDropService.java
│       │   └── OreInspectionService.java
│       ├── infrastructure/
│       │   ├── PaperOreBreakListener.java
│       │   ├── PaperOreDropDeltaNotifier.java
│       │   ├── PaperVanillaDropScanner.java
│       │   └── YamlOreDropPolicyProvider.java
│       └── presentation/
│           └── OreInspectDiagnostic.java
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
│   │   │   ├── EnvironmentalStateComposer.java
│   │   │   └── CropPerformanceService.java
│   │   ├── domain/
│   │   │   ├── CropEnvironmentalState.java
│   │   │   ├── CropPerformanceCalculator.java
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
│       ├── application/TreeGrowthService.java
│       ├── domain/
│       │   ├── TreeGrowthPolicy.java
│       │   ├── TreeGrowthPolicyProvider.java
│       │   ├── TreeGrowthChance.java
│       │   ├── TreeGrowthSeasonalFactor.java
│       │   ├── TreeGrowthChanceVariationSource.java
│       │   ├── TreeGrowthDecision.java
│       │   └── UnsupportedTreeGrowthPolicyException.java
│       └── infrastructure/
│           ├── PaperTreeGrowthListener.java
│           └── YamlTreeGrowthPolicyProvider.java
│
├── animals/
│   └── breeding/
│       ├── application/AnimalBreedingService.java
│       ├── domain/
│       │   ├── AnimalKind.java
│       │   ├── AnimalBreedingPolicy.java
│       │   └── AnimalBreedingPolicyProvider.java
│       └── infrastructure/
│           ├── PaperAnimalBreedingListener.java
│           └── YamlAnimalBreedingPolicyProvider.java
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
- `BlockPosition` — world + int x + int y + int z, used for block events and biome resolution.
- `EcologicalRegionId` — world UUID and horizontal indices in a fixed 128-by-128 block grid. Floor division gives negative coordinates their own regions.

Bukkit `Location` is translated into spatial value objects at infrastructure boundaries.

### 8.2 Biome Read Model

`BiomeContext` is the single read model for resolved biome/environment information.

It combines:

- `BiomeId` — the identity of the resolved biome.
- `BiomeProfile` — static or configured ecological properties for that biome type.
- Optional `EcologicalRegionState` — current world sky weather and clock phase for the target region in normal worlds.

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
- V1 crop environmental state composition uses biome profile humidity and
  temperature, optionally modified by season profile factors. For those
  season-modified variables, use only:

  ```text
  finalVariable = clamp01(biomeVariable * seasonVariableFactor)
  ```

- V1 soil fertility comes only from `BiomeProfile.Fertility`. Do not apply
  season or weather factors to soil fertility.
- V1 wind speed, rain strength, and solar incidence use neutral defaults until
  upstream weather or solar contracts exist.
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

## 18. Runtime Capabilities

### 18.1 Configuration and Administration

`DynamicBiomes` copies missing bundled configuration files on startup. Providers
validate configuration into typed views before Paper registers runtime listeners
and commands. Read-only commands expose the current season (`/db season`), the
biome and ecological observation at the player's position (`/db biome`), and
targeted ore and crop diagnostics (`/db inspect`).

`/db reload` requires `dynamicbiomes.admin.reload`. It validates a complete
candidate configuration before replacing the active listeners, command handlers,
and season schedule on the server thread. A failed reload leaves the active
configuration running and logs the error. Reload keeps the current season and
the ore origin repository in memory. A successful reload starts the enabled
advancement schedule from its configured initial delay.

### 18.2 Biomes and Seasons

The biome resolver supplies a configured profile and, in normal worlds, the
current world-level sky weather and clock phase for the requested ecological
region. Reading that observation needs no region scan or chunk load. The region
key describes the target's position and the observations are shared across
regions in the same world. They do not represent local precipitation, sky
exposure, or changes to soil fertility.

`season-cycle.yml` defines an ordered calendar and optional tick-based
advancement. With no calendar entry it uses spring, summer, autumn, then winter.
Each configured season must have a matching entry in `season-profiles.yml`.
Startup restores the persisted season or initializes the first calendar entry.
A reload that excludes the current season is rejected. Administrators with
`dynamicbiomes.admin.season` can use `/db season set <namespaced-id>` to select a
calendar entry. Manual and scheduled transitions atomically persist the new
season before updating the cached query.

### 18.3 Ores

Ore modification uses configured biome and ore rules and optional seasonal
factors. The listener adjusts the matching ore product in Paper's captured
drop event, respects item stack limits, and reports a delta when the event is
accepted. Unsupported biome and policy cases, player-placed ore, and Silk
Touch drops preserve the vanilla result.

Origin tracking records placed ore and persists origin changes atomically.
Pistons move origin entries as a batch in the direction reported by Paper.
After a tracked break, the next server tick clears the entry if there is no
supported ore at the position. Ore inspection delegates to
`OreInspectionService` for biome policy and origin lookup. The Paper diagnostic
renders that result without rolling a multiplier.

### 18.4 Crops

The Paper adapters support wheat, carrots, potatoes, and beetroot. The growth
and yield files can supply crop-wide rules under `crops`. Legacy biome-specific
rules remain valid and a configured rule for a biome and crop takes precedence
over the crop-wide rule. Growth and yield each preserve the vanilla result
when the biome or applicable policy is unsupported.

`CropPerformanceService` scores crop-owned preferences from `crop-profiles.yml`
against a crop environmental state. Humidity and temperature are the biome
profile fields multiplied by one plus the respective season adjustment and
clamped to 0–1. Soil fertility comes from the biome profile. Wind speed, rain
strength, and solar incidence take neutral values. Missing crop preference
profiles use neutral performance factors.

Growth applies the configured chance and crop season factor, caps that result
at one, and applies the performance growth chance factor with a final cap at
one. A surviving same-crop, single-stage growth proposal then uses the
performance growth speed factor to determine how many ages to advance.
Probabilistic rounding allows fractional factors, and the crop's remaining ages
bound the result. Paper's crop growth event does not identify the cause, so
bone meal that proposes one stage can also pass through the speed rule.

Harvest yield multiplies captured vanilla produce by the selected policy
multiplier, optional crop yield season factor, and performance harvest
quantity factor. The quantity calculator rounds probabilistically. The
harvest listener keeps captured seeds and other non-produce drops in the
event. Crop inspection reports the inputs and deterministic factors without
rolling growth or yield.

### 18.5 Trees and Animals

`tree-growth.yml` controls the chance of eligible tree generation by biome and
season. It covers sapling and propagule trees and preserves vanilla in unconfigured
biomes. The bundled forest policy allows 100% of eligible spring attempts,
80% in summer, 68% in autumn, and 40% in winter. Paper marks player bone
meal and the listener skips it. Paper 1.21.4 can report dispenser bone meal as
natural growth, which makes those attempts eligible for the configured rule.

`animal-breeding.yml` controls the seasonal chance of player-fed cow, sheep,
pig, and chicken breeding. The bundled file is neutral. Paper calls the
breeding event after the player has fed the animals, so a cancelled attempt
still consumes the food already used.

### 18.6 Deferred Design

Local environmental contributors such as wind, measured rainfall, and soil
change need separate input models. Crop quality remains outside the crop
performance model. An external plugin API and database adapter need concrete
integration and persistence requirements before implementation.

## 19. Preserved Principles

- `biome` is not a god/master domain.
- `biome` owns biome identity, resolution, profile, and plugin-owned ecological state.
- `seasons` is separate and orthogonal to `biome`.
- Feature domains such as `ore`, `crops`, `trees`, and `animals` interpret environmental context into behavior.
- Domain packages must not import Bukkit/Paper APIs.
- Bukkit, YAML, file I/O, database, and plugin lifecycle code belong outside domain.
