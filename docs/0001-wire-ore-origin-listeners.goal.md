---
id: DB-RUNTIME-001
type: runtime-composition
bounded_context: ore
capability: origin-and-drops
layer: pluginruntime
status: Ready
expected_commit: "feat(ore): wire ore origin listeners"
---

# Goal Card: Wire ore origin listeners into plugin runtime

## Status

Ready

## Goal

During plugin enable, construct one YAML-backed ore origin tracking service and register the existing ore place and break listeners with that shared service.

## Why Now

The repository already contains `YamlOreOriginRepository`, `OreOriginTrackingService`, `PaperOrePlaceListener`, and `PaperOreBreakListener`, but their runtime path must be composed and registered by the plugin startup class.

The current worktree already contains partial implementation changes in the startup class, plugin smoke test, and `ARCHITECTURE.md`. Treat those changes as current state: inspect and complete or correct them without reverting unrelated user work.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/origin/application/OreOriginTrackingService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/origin/infrastructure/YamlOreOriginRepository.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/origin/infrastructure/PaperOrePlaceListener.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/infrastructure/PaperOreBreakListener.java`
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java`

## Current State

- `YamlOreOriginRepository` accepts a plugin-selected `Path` and creates its parent directory when it first writes.
- `OreOriginTrackingService` records player-placed ore, checks multiplier eligibility, and clears tracked origins.
- `PaperOrePlaceListener` records placed iron ore through `OreOriginTrackingService`.
- `PaperOreBreakListener` currently accepts only `OreOriginTrackingService` and clears tracked iron ore origins. It does not accept or invoke `OreDropService`.
- `OreDropService` and `YamlOreDropPolicyProvider` exist, but the current break listener does not consume them and no complete production drop-calculation dependency set is justified by this wiring slice.
- The worktree already has partial listener registration and smoke-test changes that must be reviewed rather than overwritten.

## Architectural Boundary

- Bounded context: `ore`
- Capabilities: `ore/origin` and `ore/drops`
- Layer: `pluginruntime` composition/startup only

Rules:

- `pluginruntime` may import ore application and infrastructure classes only to compose and register them.
- The YAML persistence path is selected only in `pluginruntime`; YAML reading and writing remain in `ore/origin/infrastructure`.
- Both listeners receive the same `OreOriginTrackingService` instance.
- Listener classes remain infrastructure adapters and must not construct repositories, services, providers, or file paths.
- Do not construct an unused `OreDropService` or `YamlOreDropPolicyProvider`. Wire them only if an existing listener constructor already requires them without changing ore calculation behavior.
- Do not add `ModuleComposer`, a service locator, or a dependency injection framework.

## Acceptance Behavior

1. Given the packaged plugin is enabled, `PaperOrePlaceListener` is registered for that plugin.
2. Given the packaged plugin is enabled, `PaperOreBreakListener` is registered for that plugin.
3. Given startup composition, one `OreOriginTrackingService` instance is constructed from one `YamlOreOriginRepository` and shared by both listeners.
4. Given plugin-owned storage, the origin repository path is selected from `getDataFolder().toPath().resolve("ore-origins.yml")` or an equivalently scoped plugin data path.
5. Given a player-placed tracked iron ore is broken, the registered break path includes origin cleanup through the shared tracking service.
6. Given the current break-listener contract, runtime wiring does not add or change ore quantity, multiplier, biome, season, ecological, balancing, or Fortune behavior.
7. Given plugin enable, existing lifecycle log messages and packaged resource contracts remain unchanged.

## TDD Plan

1. Add or update the smallest safe packaged-plugin smoke test first to assert both listener classes are registered after enable.
2. Confirm the test fails for the expected missing-registration reason when practical. If partial worktree changes already make it pass, report that pre-existing state.
3. Implement or correct only the startup composition needed to create the repository, shared tracking service, listeners, and registrations.
4. Run focused plugin/runtime tests.
5. Run the full verification commands.

Do not add brittle reflection assertions for object identity. Shared construction should remain obvious in the startup composition code and be checked during review.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`

Tests:

- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java`

Documentation, only if the implemented runtime state changes an existing statement:

- `ARCHITECTURE.md`

These are expected files, not permission to create extra scaffolding. Constructor changes outside `pluginruntime` are not expected for the current repository state.

## Responsibility / Collaboration Notes

Class: `DynamicBiomes`

Responsibility:

- Select the plugin-owned `ore-origins.yml` path.
- Construct the YAML repository and shared tracking service.
- Construct and register the existing place and break listeners.

Must not:

- Add gameplay or domain rules.
- Parse YAML itself.
- Construct unused ore drop calculation collaborators.

Class: `PaperOrePlaceListener`

Responsibility:

- Translate placed iron ore events and record player-placed origin through the injected tracking service.

Must not:

- Choose persistence paths or construct collaborators.

Class: `PaperOreBreakListener`

Responsibility:

- Translate broken iron ore events and clear tracked origin through the injected tracking service.

Must not:

- Gain new drop-calculation behavior in this card.
- Choose persistence paths or construct collaborators.

## Out of Scope

Explicitly forbidden work:

- Biome resolver, season, or ecological-state integration.
- Ore multiplier selection or quantity calculation runtime behavior.
- Minecraft Fortune algorithm implementation.
- Database persistence, public API, or config-driven material catalog.
- Generic service locator, dependency injection framework, or `ModuleComposer`.
- Moving listener registration into infrastructure.
- Changing listener ownership or allowing non-`pluginruntime` packages to import `pluginruntime`.
- Wiring `ore-drops.yml` or `YamlOreDropPolicyProvider` when no current runtime consumer requires it.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md` or `AGENTS.md`;
- a required listener, repository, or tracking service no longer exists;
- completing the task requires adding ore drop calculation behavior or changing a listener constructor beyond simple dependency injection;
- constructing `OreDropService` would leave it unused or require inventing production variation sources;
- listener registration cannot be tested without unsafe or brittle server mocks;
- implementation requires broader architecture changes or unused scaffolding;
- existing uncommitted user changes make the required scope impossible to complete without overwriting them.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Inspect:

```bash
git diff --name-status
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java
git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/"
```

Confirm Bukkit/Paper imports remain in infrastructure or `pluginruntime`, and YAML/file-path construction remains in infrastructure or `pluginruntime`.

## Report Back

After implementation, report:

1. Files changed.
2. How the origin repository path is selected.
3. How one `OreOriginTrackingService` instance is shared by place and break listeners.
4. How both listeners are registered.
5. Whether the runtime break path is protected by origin tracking and cleanup.
6. Tests added or why listener registration testing was deferred.
7. Exact verification command results.
8. Architecture boundary risks or deferred drop-service wiring.
9. Suggested commit message.
