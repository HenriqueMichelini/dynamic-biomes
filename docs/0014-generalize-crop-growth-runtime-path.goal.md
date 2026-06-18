---
id: DB-CROPS-012
type: architecture
bounded_context: crops
capability: growth
layer: n/a
status: Ready
expected_commit: "refactor(crops): generalize crop growth runtime path"
---

# Goal Card: Generalize crop growth runtime path

## Status

Ready

## Goal

Refactor the current wheat-driven crop growth implementation into a crop-kind-driven growth path while preserving wheat-only runtime behavior.

## Why Now

The wheat crop feature is implemented, manually validated, season-aware, and observable. The next crop should not be added by duplicating wheat-specific listeners or diagnostics. Before adding carrots or other crops, align crops with the established ore pattern: infrastructure maps Bukkit materials to domain kinds, then one crop growth path handles supported kinds.

Do not justify work only because a future target architecture mentions it.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- Existing crop growth tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth`
- Existing plugin runtime tests that assert listener registration or inspect command composition

## Current State

- Crop growth behavior is currently wheat-driven.
- Wheat natural growth can be allowed or cancelled by configured chance.
- Wheat policy supports optional season-keyed factors.
- `crop-growth.yml` currently configures wheat only.
- Runtime wheat growth is already wired through the Paper listener and `DynamicBiomes`.
- `/dynamicbiomes inspect` reports wheat growth diagnostics.
- The next crop is not yet implemented.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: cross-layer refactor within the existing capability

Rules:

- Keep crop-specific behavior in `crops/growth`, not `biome`.
- Keep Bukkit `Material` recognition in infrastructure.
- Keep YAML parsing in infrastructure.
- Keep domain/application free of Bukkit/Paper/YAML/file I/O/pluginruntime imports.
- Keep pluginruntime limited to construction and registration.
- Do not create duplicate per-crop listeners such as `PaperCarrotGrowthListener`.

## Acceptance Behavior

1. Given the current wheat configuration, when wheat grows naturally in a configured biome and season, then runtime allow/cancel behavior is unchanged.
2. Given `Material.WHEAT`, when infrastructure maps the block material, then it resolves to the domain crop kind for wheat.
3. Given an unsupported Bukkit material, when the generic crop growth listener receives a `BlockGrowEvent`, then it ignores the event without calling the crop growth service.
4. Given a wheat `BlockGrowEvent`, when the generic crop growth listener receives it, then it delegates to the crop growth service with the wheat crop kind and cancels only on `CANCEL_GROWTH`.
5. Given the existing `crop-growth.yml` wheat shape, when the YAML provider loads policy data, then the file remains backward-compatible and still loads wheat policy and seasonal factors.
6. Given `/dynamicbiomes inspect` targets wheat, then the existing wheat diagnostic content remains available through the generalized crop diagnostic path.
7. Given carrots, potatoes, beetroots, or other crops, then they remain unsupported unless already supported before this refactor.

## TDD Plan

1. Add or update tests proving the material mapper recognizes wheat and rejects unsupported materials.
2. Add or update listener tests proving the generic crop listener preserves wheat behavior and ignores unsupported materials.
3. Add or update service/provider/diagnostic tests only as needed to support crop-kind-driven APIs while preserving current wheat behavior.
4. Confirm the focused tests fail for the expected reason when practical.
5. Rename/refactor the smallest production surface needed to make the tests pass.
6. Run focused crop-growth tests.
7. Run plugin runtime tests affected by listener registration or inspect composition.
8. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKind.java`
- Current wheat-specific domain classes renamed or adapted to crop-growth vocabulary where needed
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/CropGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperCropMaterialMapper.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperCropGrowthListener.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlCropGrowthPolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/CropGrowthInspectDiagnostic.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `ARCHITECTURE.md` if implemented package/class names or runtime behavior documentation changes

Tests:

- Crop growth domain tests renamed or adapted as needed
- Crop growth application tests renamed or adapted as needed
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperCropMaterialMapperTest.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperCropGrowthListenerTest.java`
- Crop growth YAML provider tests renamed or adapted as needed
- Crop growth inspect diagnostic tests renamed or adapted as needed
- Affected plugin runtime tests

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `CropKind`

Responsibility:

- Represent the crop kind known by the crop growth domain.
- Initially include only currently supported runtime behavior, expected to be wheat.

Collaborators:

- `CropGrowthPolicyProvider`
- `CropGrowthService`
- `PaperCropMaterialMapper`

Must not:

- Import Bukkit/Paper.
- Encode YAML keys as framework details beyond stable crop identity.

Class: `PaperCropMaterialMapper`

Responsibility:

- Translate Bukkit `Material` values into domain `CropKind` values.
- Return empty for unsupported materials.

Collaborators:

- `PaperCropGrowthListener`
- `CropKind`

Must not:

- Decide growth.
- Read YAML.
- Import application services beyond mapper tests if avoidable.

Class: `PaperCropGrowthListener`

Responsibility:

- Handle natural crop `BlockGrowEvent` for supported crop materials.
- Translate block location to `BlockPosition`.
- Delegate to `CropGrowthService` with the resolved `CropKind`.
- Cancel only when the service returns `CANCEL_GROWTH`.

Collaborators:

- `PaperCropMaterialMapper`
- `CropGrowthService`

Must not:

- Contain chance logic.
- Read YAML.
- Resolve biome directly except through the service.
- Add per-crop branching beyond material mapping.

Class: `CropGrowthInspectDiagnostic`

Responsibility:

- Report read-only crop growth diagnostics for supported crop target blocks.
- Preserve existing wheat diagnostic behavior.

Collaborators:

- `PaperCropMaterialMapper` or an equivalent infrastructure-facing mapping boundary
- `CropGrowthPolicyProvider`
- `CurrentSeasonQuery`
- Published biome resolver/profile contracts

Must not:

- Roll growth decisions.
- Mutate state.
- Depend on ore packages.

## Out of Scope

Explicitly forbidden work:

- Adding carrot runtime behavior.
- Adding carrots, potatoes, beetroots, or other crop policies to `crop-growth.yml`.
- Adding a separate `PaperCarrotGrowthListener` or any other per-crop listener.
- Adding crop-specific fields to biome profiles.
- Changing the YAML schema except class/provider naming required by this refactor.
- Changing balancing values.
- Changing season advancement behavior.
- Adding bonemeal-specific behavior.
- Adding crop growth acceleration beyond vanilla natural growth cancellation.
- Adding reload commands.
- Adding public API.
- Adding persistence.
- Running or requiring a no-edit audit as part of this card.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- the repository is not actually in a wheat-driven crop state after the rollback;
- required wheat crop classes are missing or already generalized in a way that makes this card obsolete;
- implementation requires adding a second crop kind to prove the refactor;
- implementation requires per-crop listener duplication;
- implementation requires broader command-routing changes unrelated to crop diagnostics;
- tests cannot be written without introducing unsafe or unrelated tooling;
- the task would create unused scaffolding;
- runtime wheat behavior would change.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests 'io.github.henriquemichelini.dynamicbiomes.crops.growth.*' --no-daemon
cd java && ./gradlew pluginTest --tests 'io.github.henriquemichelini.dynamicbiomes.pluginruntime.DynamicBiomesPluginTest' --no-daemon
cd java && ./gradlew test build --no-daemon
```

Also run architecture-sensitive import checks relevant to this refactor:

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
4. Confirmation that wheat runtime behavior is preserved.
5. Confirmation that no second crop was added.
6. Architecture boundary risks.
7. Deferred work.
