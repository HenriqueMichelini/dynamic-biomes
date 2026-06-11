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
                 │   wires modules, owns startup only   │
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
                 │   biome, seasons, spatial            │
                 │ describe environmental conditions    │
                 └─────────────────────────────────────┘
```

Rules:

- `pluginruntime` may import all modules, but **only for composition/startup**.
- **No module imports `pluginruntime`.**
- **`biome` and `seasons` must not import feature domains.**
- Feature domains may import upstream vocabulary/ports, but **never upstream infrastructure**.
- Upstream contexts do not depend on downstream contexts.

## 4. Anti-Coupling Rules

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

Acceptable inside `biome/profile` or `biome/dynamics`:

```java
// Acceptable
BiomeTag
ClimateProfile
Humidity
Temperature
Fertility
MineralRichness
EcologicalPressure
EcologicalRegionState
```

Do **not** introduce `BiomeEffect`, `EffectType`, or any biome-owned rule vocabulary such as `ORE_DROP_MULTIPLIER`.

Configuration files must be split by ownership:

- `biome-profiles.yml` belongs to `biome/profile`
- `season-profiles.yml` belongs to `seasons/profile`
- `ore-drops.yml` belongs to `ore/drops`

Raw YAML must not leak into domain. Bukkit types must not leak into domain.

## 5. Target Structure

```text
io.github.henriquemichelini.dynamicbiomes/
├── spatial/
│   └── positioning/
│       └── domain/
│           ├── WorldReference.java
│           ├── BlockPosition.java
│           ├── ChunkPosition.java
│           └── EcologicalRegionId.java
│
├── biome/
│   ├── identity/
│   │   └── domain/
│   │       └── BiomeId.java
│   ├── resolution/
│   │   ├── domain/
│   │   │   ├── BiomeResolver.java
│   │   │   ├── BiomeContext.java
│   │   │   └── ResolvedBiome.java
│   │   └── infrastructure/
│   │       └── BukkitBiomeResolver.java
│   ├── profile/
│   │   ├── domain/
│   │   │   ├── BiomeProfile.java
│   │   │   ├── BiomeTag.java
│   │   │   ├── ClimateProfile.java
│   │   │   └── BiomeProfileProvider.java
│   │   └── infrastructure/
│   │       └── YamlBiomeProfileProvider.java
│   └── dynamics/
│       ├── domain/
│       │   ├── EcologicalRegionState.java
│       │   └── EcologicalRegionStateRepository.java
│       └── infrastructure/
│           └── YamlEcologicalRegionStateRepository.java
│
├── seasons/
│   ├── identity/
│   │   └── domain/
│   │       └── SeasonId.java
│   ├── cycle/
│   │   ├── domain/
│   │   │   ├── SeasonCalendar.java
│   │   │   ├── CurrentSeasonQuery.java
│   │   │   └── SeasonStateRepository.java
│   │   └── infrastructure/
│   │       └── BukkitCurrentSeasonQuery.java
│   │       └── YamlSeasonStateRepository.java
│   └── profile/
│       ├── domain/
│       │   ├── SeasonProfile.java
│       │   └── SeasonProfileProvider.java
│       └── infrastructure/
│           └── YamlSeasonProfileProvider.java
│
├── ore/
│   ├── identity/
│   │   └── domain/
│   │       └── OreKind.java
│   ├── origin/
│   │   ├── domain/
│   │   │   ├── OreOrigin.java
│   │   │   ├── OreOriginRepository.java
│   │   │   └── OrePlacementOriginPolicy.java
│   │   ├── application/
│   │   │   └── OreOriginTrackingService.java
│   │   └── infrastructure/
│   │       └── PaperOrePlaceListener.java
│   └── drops/
│       ├── domain/
│       │   ├── OreDropRequest.java
│       │   ├── OreDropOutcome.java
│       │   ├── OreDropPolicy.java
│       │   ├── OreDropPolicyProvider.java
│       │   ├── SilkTouchPolicy.java
│       │   └── OreDropCalculator.java
│       ├── application/
│       │   └── OreDropService.java
│       └── infrastructure/
│           ├── PaperOreBreakListener.java
│           └── YamlOreDropPolicyProvider.java
│
├── configuration/
│   └── yaml/
│       └── infrastructure/
│           ├── YamlConfigurationLoader.java
│           └── YamlValidationReporter.java
│
├── persistence/
│   └── yaml/
│       └── infrastructure/
│           ├── YamlStore.java
│           ├── AtomicYamlWriter.java
│           └── DataVersion.java
│
└── pluginruntime/
    ├── lifecycle/
    │   └── infrastructure/
    │       └── DynamicBiomes.java
    ├── composition/
    │   └── application/
    │       └── ModuleComposer.java
    └── api/
        └── DynamicBiomesApi.java
```

### 5.1 Spatial Value Objects

Replace generic `WorldPosition` with explicit spatial value objects:

- `WorldReference` — pure Java representation of a world; prefer UUID for identity; may carry name for diagnostics.
- `BlockPosition` — world + int x + int y + int z; used for block events and biome resolution.
- `ChunkPosition` — world + int chunkX + int chunkZ; used for chunk-level state if needed.
- `EcologicalRegionId` — world + regionX + regionZ; used for plugin-owned ecological state.

Bukkit `Location` is translated into these VOs at infrastructure boundaries.

### 5.2 Biome Dynamics = Ecological Region State

`biome/dynamics` owns plugin-owned ecological region state, not vanilla biome state.

- `EcologicalRegionState` — dynamic state for a specific world region. Two `minecraft:plains` areas may have different state.
- `BiomeProfile` — static or configured ecological properties for a biome type.
- `BiomeContext` or `ResolvedBiomeContext` — read model combining `BiomeId` + `BiomeProfile` + optional `EcologicalRegionState`.

Dynamic state must not contain ore, crop, or animal rule results.

### 5.3 Ore Origin Separate from Ore Drops

- `ore/origin` owns origin state and origin persistence (`OreOrigin`, `OreOriginRepository`).
- `PaperOrePlaceListener` lives in `ore/origin/infrastructure`.
- `ore/drops` consumes ore origin data but does not own origin tracking.
- `OreOrigin` uses pure domain types only; no Bukkit imports.

## 6. Persistence and Configuration Are Infrastructure

### 6.1 Persistence

Persistence is infrastructure, not a bounded context. The domain that owns the state owns the repository port.

State repository ports live inside the owning domain/capability:

```text
biome/dynamics/domain/EcologicalRegionStateRepository.java
ore/origin/domain/OreOriginRepository.java
seasons/cycle/domain/SeasonStateRepository.java
```

Persistence adapters implement those ports:

```text
biome/dynamics/infrastructure/YamlEcologicalRegionStateRepository.java
ore/origin/infrastructure/YamlOreOriginRepository.java
seasons/cycle/infrastructure/YamlSeasonStateRepository.java
```

Shared persistence utilities, if needed, are technical support infrastructure only:

```text
persistence/
└── yaml/
    └── infrastructure/
        ├── YamlStore.java
        ├── AtomicYamlWriter.java
        └── DataVersion.java
```

There is no `ecologypersistence/snapshot/domain/Snapshot.java` in the target design.
Persistence format is an implementation detail, not a domain boundary.

### 6.2 Configuration

Configuration is infrastructure, not a bounded context. Each feature/environmental context owns its own typed configuration interpretation.

Typed providers per capability:

```text
biome/profile/infrastructure/YamlBiomeProfileProvider.java
ore/drops/infrastructure/YamlOreDropPolicyProvider.java
seasons/profile/infrastructure/YamlSeasonProfileProvider.java
```

Shared YAML loading/parsing, if needed, is infrastructure support only:

```text
configuration/
└── yaml/
    └── infrastructure/
        ├── YamlConfigurationLoader.java
        └── YamlValidationReporter.java
```

There is no generic `ConfigurationProvider` port in domain. Raw YAML is translated into typed domain objects at infrastructure boundaries.

## 7. Port-Adapter Naming Convention

Not every port is a repository. Use naming that reflects responsibility:

| Pattern | Use For | Example |
|---|---|---|
| **Repository** | Persisted collections of domain objects or mutable state | `EcologicalRegionStateRepository`, `OreOriginRepository` |
| **Provider** | Configured policy/profile data | `OreDropPolicyProvider`, `BiomeProfileProvider`, `SeasonProfileProvider` |
| **Resolver** | Mapping one concept to another | `BiomeResolver` maps `BlockPosition` to `ResolvedBiome` |
| **Query / Clock** | Current temporal state | `CurrentSeasonQuery` |

## 8. Presentation Layer Is Optional

Use `presentation/` only when a capability has commands, admin GUI, public UI, or other presentation-specific adapters.

Good:

```text
ore/drops/
├── domain/
├── application/
└── infrastructure/
```

Add `presentation/` only when needed. Do not create empty layer packages preemptively.

The public plugin API for other plugins is **not** presentation. It lives under `pluginruntime/api/` (or `pluginapi/publicapi/` if separated).

## 9. Migration Plan

### Section A — Architecture-Only Refactor

Safe, reviewable, no runtime behavior change:

1. Correct package structure to match target layout.
2. Rename `BiomeState` → `EcologicalRegionState` (or explicitly define it as region-scoped).
3. Extract pure spatial value objects (`BlockPosition`, `ChunkPosition`, `EcologicalRegionId`, `WorldReference`).
4. Move `PaperOrePlaceListener` from `ore/drops/infrastructure` to `ore/origin/infrastructure`.
5. Remove Bukkit imports from all domain packages.
6. Rename ports to match naming convention (`Repository` for state, `Provider` for config, `Resolver` for mapping).
7. Split generic `Snapshot`/`SnapshotRepository` into domain-owned repository ports.
8. Replace generic `ConfigurationProvider` with typed capability-owned providers.
9. Ensure test package parity.

### Section B — Future Implementation (Out of Scope for Current Refactor)

- Listener registration in Bukkit.
- Runtime composition and `ModuleComposer` wiring.
- Actual YAML loading and parsing.
- Actual season progression.
- Actual ore drop behavior calculation.
- Bukkit integration beyond existing empty listener shells.

## 10. Preserved Principles

- `biome` is not a god/master domain.
- `biome` owns biome identity, resolution, profile, and plugin-owned ecological state.
- `seasons` is separate and orthogonal to `biome`.
- Feature domains such as `ore`, `crops`, `trees`, and `animals` interpret environmental context into behavior.
- Domain packages must not import Bukkit/Paper APIs.
- Bukkit, YAML, file I/O, database, and plugin lifecycle code belong outside domain.
