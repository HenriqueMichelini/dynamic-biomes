---
id: DB-CROPS-012
type: infrastructure
bounded_context: crops
capability: growth
layer: infrastructure
status: Ready
expected_commit: "feat(crops): add carrot growth policy support"
---

# Goal Card: Add carrot growth policy support

## Status

Ready

## Goal

Add configured carrot growth policy support to the existing crop-growth pipeline while preserving current wheat runtime behavior.

## Why Now

The crop-growth model has been generalized away from wheat-specific domain and service names, but runtime behavior is still wheat-only. The next justified step is to introduce a real second configured crop policy so future carrot runtime wiring can reuse the same crop-growth pipeline instead of copying the wheat implementation.

Do not justify additional crop kinds, runtime listeners, or command behavior only because the generalized model could support them later.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/CropGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlCropGrowthPolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperWheatGrowthListener.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnostic.java`
- `java/app/src/main/resources/crop-growth.yml`
- relevant crop-growth tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth`

## Current State

- `CropGrowthPolicy` represents a configured base chance plus optional season-keyed factors.
- `CropGrowthService` evaluates crop growth through biome resolution, policy lookup, current season, and domain decision logic.
- `YamlCropGrowthPolicyProvider` loads the current wheat policy from `crop-growth.yml`.
- `PaperWheatGrowthListener` is registered at runtime and applies the crop-growth service only to wheat blocks.
- `WheatGrowthInspectDiagnostic` reports wheat policy diagnostics only.
- No second crop is configured, inspected, or handled at runtime.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: primarily `infrastructure`, with the minimum domain/application API changes required to select a crop kind

Rules:

- Crop-specific growth behavior remains owned by `crops/growth`, not `biome` or `seasons`.
- Bukkit/Paper imports remain only in infrastructure/presentation adapter code and tests that already use Bukkit mocks.
- YAML/file I/O remains only in infrastructure.
- Domain/application must not import Bukkit/Paper, YAML, file I/O, or `pluginruntime`.
- `PaperWheatGrowthListener` must remain wheat-only in this card.
- Existing wheat runtime behavior must remain unchanged.

## Acceptance Behavior

1. Given a supported biome with a configured `wheat` policy, when the existing wheat runtime path evaluates growth, then behavior remains unchanged.
2. Given a supported biome with a configured `carrots` policy, when the provider is asked for the carrot policy, then it returns a `CropGrowthPolicy` with the configured base chance and optional seasonal factors.
3. Given a supported biome with no configured `carrots` policy, when the provider is asked for the carrot policy, then it throws the existing unsupported crop-growth policy exception.
4. Given an unsupported crop key in YAML, when the provider loads the file, then it rejects the unsupported crop key explicitly rather than silently accepting it.
5. Given carrot policy data with invalid chance or seasonal factor values, when the provider loads the policy, then it fails with the same strict validation behavior used for wheat.
6. Given `CropGrowthService` is asked to evaluate a crop kind, then it passes that crop kind to the policy provider and uses the returned policy for the existing season-aware decision path.
7. Given the current wheat listener and wheat inspect diagnostic, then they explicitly request the wheat crop kind and continue to pass existing tests.
8. Given carrot support is configured in YAML, no carrot runtime event listener is registered and no carrot block behavior changes in-game during this card.

## TDD Plan

1. Add or update the smallest provider and service tests first:
   - carrot policy loads from YAML;
   - missing carrot policy falls back through the existing unsupported exception path;
   - invalid carrot values are rejected;
   - service passes the requested crop kind to the provider;
   - wheat runtime adapter tests still use the wheat crop kind.
2. Confirm the focused tests fail for the expected reason when practical.
3. Add the smallest domain/application API needed to represent a supported crop kind, for example `CropKind` with wheat and carrots only.
4. Update the provider, service, wheat listener, and wheat diagnostic to use the explicit crop kind.
5. Update `crop-growth.yml` with a default `carrots` policy only after tests describe the expected shape.
6. Run focused tests.
7. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKind.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthPolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/CropGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlCropGrowthPolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperWheatGrowthListener.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnostic.java`
- `java/app/src/main/resources/crop-growth.yml`
- `ARCHITECTURE.md` only if implemented package/resource ownership or runtime-support documentation changes

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKindTest.java` if `CropKind` has validation/parsing behavior beyond an enum constant list
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/CropGrowthServiceTest.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlCropGrowthPolicyProviderTest.java`
- existing wheat listener and inspect tests as needed

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `CropKind`

Responsibility:

- Represents the explicitly supported crop growth policy keys for this capability.

Collaborators:

- `CropGrowthPolicyProvider`
- `YamlCropGrowthPolicyProvider`
- `CropGrowthService`
- wheat-specific adapters requesting `CropKind.WHEAT`

Must not:

- Depend on Bukkit `Material`.
- Become a generic registry for every Minecraft plant.
- Include behavior for unsupported future crops.

Class: `YamlCropGrowthPolicyProvider`

Responsibility:

- Load crop-owned growth policies by biome and crop kind.

Collaborators:

- `BiomeId`
- `CropKind`
- `CropGrowthPolicy`
- `CropGrowthChance`
- `CropGrowthSeasonalFactor`

Must not:

- Resolve Bukkit materials.
- Decide growth outcomes.
- Read current season.
- Register runtime listeners.

Class: `CropGrowthService`

Responsibility:

- Orchestrate biome resolution, configured crop policy lookup, current season lookup, and domain decision for a supplied crop kind.

Collaborators:

- `BiomeResolver`
- `CropGrowthPolicyProvider`
- `CurrentSeasonQuery`
- `CropGrowthPolicy`

Must not:

- Contain crop balancing rules.
- Import Bukkit/Paper or YAML.
- Know about concrete listener classes.

## Out of Scope

Explicitly forbidden work:

- Registering or implementing a `PaperCarrotGrowthListener`.
- Making carrots change growth behavior at runtime.
- Extending `/dynamicbiomes inspect` to report carrot diagnostics.
- Adding potatoes, beetroots, nether wart, pumpkins, melons, saplings, trees, or animals.
- Adding bonemeal/player-triggered behavior.
- Changing the wheat chance, wheat seasonal factors, or existing wheat runtime semantics.
- Adding public API, reload commands, persistence, or database behavior.
- Adding generic biome-owned crop fields such as `cropGrowthSpeed` or `carrotGrowthChance`.
- Introducing a broad crop registry or speculative package structure.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- the current provider/service APIs are materially different from this card’s assumptions;
- supporting carrots requires broad runtime listener registration in the same slice;
- the implementation would create unused scaffolding beyond the explicit carrot policy support;
- the implementation requires changing biome or season ownership rules;
- tests cannot be written without unsafe or unrelated tooling;
- existing wheat runtime behavior would change without an explicit test proving equivalence.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Run focused tests for the touched crop-growth provider/service/listener/diagnostic classes before the full command.

Also inspect architecture-sensitive imports:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/"
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether wheat runtime behavior remained unchanged.
5. Whether carrots are configured only or runtime-active.
6. Architecture boundary risks.
7. Deferred work.
