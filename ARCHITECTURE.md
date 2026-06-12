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
                 │   biome, seasons                     │
                 │ describe environmental conditions    │
                 └─────────────────────────────────────┘
```

Rules:

- `pluginruntime` may import all modules, but **only for composition/startup**.
- **No module imports `pluginruntime`.**
- **`biome` and `seasons` must not import feature domains.**
- Feature domains may import upstream vocabulary/ports, but **never upstream infrastructure**.
- Upstream contexts do not depend on downstream contexts.

## 4. Published Language

Downstream feature contexts must consume only explicitly published upstream contracts. Feature domains must not import arbitrary internal packages from `biome` or `seasons`.

### 4.1 What Is Published

- **Biome identity and resolution**: `BiomeId`, `BiomeContext`, `BiomeResolver` port.
- **Biome static profile**: `BiomeProfile`, `BiomeProfileProvider` port, `BiomeTag`, `ClimateProfile`, `Humidity`, `Temperature`, `Fertility`, `MineralRichness`, `EcologicalPressure`.
- **Season identity and query**: `SeasonId`, `CurrentSeasonQuery` port, `SeasonProfile`, `SeasonProfileProvider` port.
- **Spatial vocabulary**: `WorldReference`, `BlockPosition`, `ChunkPosition`, `EcologicalRegionId`.
- **Dynamic ecological state**: `EcologicalRegionState` as an entity concept; exposed to downstream contexts through `BiomeContext` or explicit read/query ports. The repository port is owned by `biome/dynamics` and should not be used by feature domains for arbitrary mutation.

### 4.2 What Is Not Published

- Internal infrastructure packages: `biome/*/infrastructure`, `seasons/*/infrastructure`.
- Internal repository implementations: `YamlEcologicalRegionStateRepository`, `BukkitBiomeResolver`.
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

## 8. Target Structure

```text
io.github.henriquemichelini.dynamicbiomes/
├── spatial/
│   └── domain/
│       ├── WorldReference.java
│       ├── BlockPosition.java
│       ├── ChunkPosition.java
│       └── EcologicalRegionId.java
│
├── biome/
│   ├── identity/
│   │   └── domain/
│   │       └── BiomeId.java
│   ├── resolution/
│   │   ├── domain/
│   │   │   ├── BiomeResolver.java
│   │   │   └── BiomeContext.java
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
│   │       ├── BukkitCurrentSeasonQuery.java
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
    └── composition/
        └── application/
            └── ModuleComposer.java
```

### 8.1 Spatial Value Objects

Replace generic `WorldPosition` with explicit spatial value objects:

- `WorldReference` — pure Java representation of a world; prefer UUID for identity; may carry name for diagnostics.
- `BlockPosition` — world + int x + int y + int z; used for block events and biome resolution.
- `ChunkPosition` — world + int chunkX + int chunkZ; used for chunk-level state if needed.
- `EcologicalRegionId` — world + regionX + regionZ; used for plugin-owned ecological state.

Optional spatial value objects (such as `ChunkPosition`) should not be implemented until used. Bukkit `Location` is translated into these VOs at infrastructure boundaries.

### 8.2 Biome Read Model

`BiomeContext` is the single read model for resolved biome/environment information.

It combines:

- `BiomeId` — the identity of the resolved biome.
- `BiomeProfile` — static or configured ecological properties for that biome type.
- Optional `EcologicalRegionState` — plugin-owned dynamic ecological state for the specific region.

There is no separate `ResolvedBiome`. `BiomeResolver` returns `BiomeContext`.

Dynamic state must not contain ore, crop, or animal rule results.

### 8.3 Ore Origin Separate from Ore Drops

- `ore/origin` owns origin state and origin persistence (`OreOrigin`, `OreOriginRepository`).
- `PaperOrePlaceListener` lives in `ore/origin/infrastructure`.
- `ore/drops` consumes ore origin data but does not own origin tracking.
- `OreOrigin` uses pure domain types only; no Bukkit imports.

## 9. EcologicalRegionState

`EcologicalRegionState` is plugin-owned dynamic ecological state for a specific region.

Properties:

- Identified by `EcologicalRegionId`.
- Treated as an **entity/aggregate concept**, not a passive data bag.
- Encapsulates its own state transitions and invariants.
- Must not contain feature-specific results such as `oreMultiplier`, `cropGrowthSpeed`, or `animalDeathChance`.

Acceptable contents:

- `Humidity`, `Temperature`, `Fertility`, `MineralRichness`, `EcologicalPressure` — environmental condition value objects owned by `biome/profile` and referenced here as dynamic state values that may change over time.
- Transition methods that validate state changes against domain rules.

## 10. Value Object Rules

- **No empty wrappers.** A value object must carry meaningful, validated data.
- **Validate construction invariants.** IDs cannot be null or blank. Namespaced keys must be valid. Coordinate objects must use explicit integer block/chunk/region semantics.
- **Records are appropriate** for immutable value objects (`BiomeId`, `BlockPosition`, `SeasonId`, etc.).
- **Entities/aggregates should not be passive records** unless immutability and all transition methods are explicitly modeled. `EcologicalRegionState` is an entity; it should encapsulate behavior.
- **Equality is by value** for value objects, by identity for entities.

## 11. Persistence and Configuration Are Infrastructure

### 11.1 Persistence

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

### 11.2 Configuration

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

## 12. Port-Adapter Naming Convention

Not every port is a repository. Use naming that reflects responsibility:

| Pattern | Use For | Example |
|---|---|---|
| **Repository** | Persisted collections of domain objects or mutable state | `EcologicalRegionStateRepository`, `OreOriginRepository` |
| **Provider** | Configured policy/profile data | `OreDropPolicyProvider`, `BiomeProfileProvider`, `SeasonProfileProvider` |
| **Resolver** | Mapping one concept to another | `BiomeResolver` maps `BlockPosition` to `BiomeContext` |
| **Query / Clock** | Current temporal state | `CurrentSeasonQuery` |

## 13. Domain Events

Domain events are allowed for cross-context reactions.

Appropriate uses:

- `SeasonTransitioned` — downstream domains react to season changes.
- `EcologicalRegionStateChanged` — downstream domains react to ecological state transitions.

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
- Example: `InMemoryBiomeProfileProvider`, `InMemoryEcologicalRegionStateRepository`, `FixedCurrentSeasonQuery`.
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

## 18. Migration Plan

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
9. Consolidate biome read model to `BiomeContext`; remove `ResolvedBiome`.
10. Ensure test package parity.

### Section B — Future Implementation (Out of Scope for Current Refactor)

- Broader runtime composition beyond the existing ore origin listener wiring.
- Actual YAML-backed policy loading and parsing.
- Actual season progression.
- Actual ore drop behavior calculation.
- Bukkit integration beyond the existing ore origin place/break listener adapters.

## 19. Preserved Principles

- `biome` is not a god/master domain.
- `biome` owns biome identity, resolution, profile, and plugin-owned ecological state.
- `seasons` is separate and orthogonal to `biome`.
- Feature domains such as `ore`, `crops`, `trees`, and `animals` interpret environmental context into behavior.
- Domain packages must not import Bukkit/Paper APIs.
- Bukkit, YAML, file I/O, database, and plugin lifecycle code belong outside domain.
