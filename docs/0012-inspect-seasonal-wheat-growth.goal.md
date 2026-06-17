---
id: DB-CROPS-010
type: presentation
bounded_context: crops
capability: growth
layer: presentation
status: Ready
expected_commit: "feat(crops): inspect seasonal wheat growth"
---

# Goal Card: Inspect seasonal wheat growth diagnostics

## Status

Ready

## Goal

Update `/dynamicbiomes inspect` wheat diagnostics so they report the current season, seasonal growth factor, effective wheat growth chance, and vanilla-fallback behavior without rolling or mutating state.

## Why Now

Wheat growth runtime now applies crop-owned seasonal factors through `WheatGrowthService` and `CurrentSeasonQuery`. The existing wheat inspect diagnostics were created before that runtime behavior was activated, so they can report the base configured chance while omitting the current-season adjustment that actually affects natural wheat growth decisions.

This card aligns read-only observability with the implemented runtime behavior. It does not add new crop behavior, new YAML schema, new crop kinds, commands, reloads, or balancing rules.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0008-inspect-wheat-growth-policy.goal.md`
- `docs/0009-wheat-seasonal-growth-factor.goal.md`
- `docs/0010-yaml-wheat-seasonal-growth-factors.goal.md`
- `docs/0011-apply-seasonal-wheat-growth-runtime.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthSeasonalFactor.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnostic.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnosticTest.java`

## Current State

- `/dynamicbiomes inspect` has wheat growth diagnostics through a crop-owned presentation diagnostic and neutral pluginruntime inspect command composition.
- Wheat diagnostics report the configured base wheat growth chance and vanilla-fallback status.
- `WheatGrowthChancePolicy` supports `effectiveChanceFor(SeasonId)` and `decide(SeasonId)`.
- `YamlWheatGrowthChancePolicyProvider` loads optional `wheat.seasonal-factors` from `crop-growth.yml`.
- Runtime wheat growth decisions now use the current season through `WheatGrowthService`.
- Inspect diagnostics do not yet show the current season or season-adjusted effective chance.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `presentation`

Rules:

- Crop growth diagnostics must remain crop-owned, not ore-owned, biome-owned, or season-owned.
- `pluginruntime` may wire diagnostics only for command composition/startup.
- Do not make `ore/drops` depend on `crops/growth`.
- Crop presentation may use published upstream contracts, including `BiomeResolver` and `CurrentSeasonQuery`.
- Crop domain/application must not import Bukkit/Paper, YAML, file I/O, or pluginruntime.
- Diagnostics must read policy data and current season only; they must not roll randomness, simulate growth, or mutate state.

## Acceptance Behavior

1. Given a player inspects wheat in a supported biome with a configured wheat policy and a configured factor for the current season, then the output reports the target block, resolved biome/profile support, wheat policy support, configured base chance, current season, seasonal factor, effective growth chance, and whether DynamicBiomes may cancel natural growth.
2. Given base chance `0.50`, current season `minecraft:winter`, and seasonal factor `0.50`, then the output reports effective growth chance `0.25` and `May cancel natural growth: yes`.
3. Given base chance `0.75`, current season `minecraft:spring`, and seasonal factor `2.00`, then the output reports effective growth chance `1.0` or equivalent capped formatting and `May cancel natural growth: no`.
4. Given a current season with no configured seasonal factor, then the output reports that the seasonal factor is not configured or defaults to `1.0`, reports the effective chance equal to the configured base chance, and preserves the existing support diagnostics.
5. Given an unsupported biome, then wheat diagnostics report vanilla fallback and must not require a configured policy or seasonal factor.
6. Given a supported biome with no configured wheat policy, then wheat diagnostics report unsupported policy and vanilla fallback.
7. Given a real failure from the biome resolver, provider, current season query, or policy read path outside explicit unsupported fallback cases, then the failure is not swallowed.
8. Given a non-wheat target block, existing ore inspect behavior remains unchanged.
9. Inspect diagnostics do not call `WheatGrowthChanceVariationSource`, do not call `WheatGrowthService`, do not call `decide(...)`, and do not mutate any repository or runtime state.

Suggested user-facing lines may be adjusted to match the existing command style, but the information must be explicit:

```text
Wheat growth policy: supported
Configured wheat growth chance: 0.75
Current season: minecraft:spring
Seasonal wheat growth factor: 1.20
Effective wheat growth chance: 0.90
May cancel natural growth: yes
```

For fallback cases, keep explicit vanilla-preserving wording such as:

```text
Wheat growth policy: unsupported
May cancel natural growth: no (vanilla fallback)
```

## TDD Plan

1. Add or update focused `WheatGrowthInspectDiagnosticTest` cases first.
2. Cover supported wheat diagnostics with a configured current-season factor.
3. Cover capping behavior where the effective chance reaches `1.0`.
4. Cover missing seasonal factor fallback to base chance.
5. Cover unsupported biome and unsupported policy fallback behavior remains unchanged.
6. Cover that diagnostics do not roll, decide, or mutate state where existing fakes make that observable.
7. Confirm the focused tests fail for the expected reason when practical.
8. Implement the smallest production change.
9. Update runtime command composition only if `CurrentSeasonQuery` must be passed into the diagnostic.
10. Run focused tests.
11. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnostic.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java` only if a small read-only accessor is needed for diagnostics
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java` only if the existing `CurrentSeasonQuery` must be wired into the diagnostic

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnosticTest.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyTest.java` only if a new read-only domain accessor is added
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomesInspectCommandExecutorTest.java` only if command composition changes
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java` only if runtime command wiring changes in an observable way

Documentation:

- `ARCHITECTURE.md` only if the implemented presentation/runtime observability described there changes.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `WheatGrowthInspectDiagnostic`

Responsibility:

- Detect wheat target diagnostics.
- Resolve biome support through the published `BiomeResolver` contract.
- Query `WheatGrowthChancePolicyProvider` for the resolved biome.
- Query `CurrentSeasonQuery` only after policy support is established.
- Report base chance, current season, seasonal factor/default, effective chance, and cancellation possibility.
- Preserve explicit unsupported biome/policy vanilla fallback output.

Collaborators:

- `BiomeResolver`
- `WheatGrowthChancePolicyProvider`
- `CurrentSeasonQuery`
- `WheatGrowthChancePolicy`

Must not:

- Roll a variation source.
- Call `WheatGrowthService` if that hides diagnostic details.
- Call `decide(...)` when effective chance can be read directly.
- Mutate any repository or runtime state.
- Add crop-specific fields to biome profiles or season profiles.
- Make ore presentation depend on crops.

Class: `WheatGrowthChancePolicy`

Responsibility, only if needed:

- Expose minimal read-only seasonal factor information required by diagnostics.

Must not:

- Add presentation formatting.
- Import presentation/application/infrastructure code.
- Change existing decision semantics.

## Out of Scope

Explicitly forbidden work:

- Adding carrot, potato, beetroot, melon, pumpkin, tree, or animal support.
- Changing `crop-growth.yml` schema or default values.
- Changing `season-profiles.yml` or `season-cycle.yml`.
- Changing runtime growth decision semantics.
- Adding crop acceleration mechanics.
- Adding bonemeal/player-triggered behavior.
- Adding action bar feedback.
- Adding reload commands.
- Adding public API.
- Adding database persistence.
- Refactoring the command system beyond the smallest composition update needed for this diagnostic.
- Performing a no-edit audit as part of this slice.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- seasonal inspect diagnostics would require `ore/drops` to import `crops/growth`;
- current command composition cannot pass `CurrentSeasonQuery` without broad lifecycle or command architecture refactoring;
- `CurrentSeasonQuery` is not available as a published seasons contract in the current code;
- reporting seasonal factor requires changing YAML schema or runtime behavior;
- a read-only domain accessor cannot be added without weakening domain invariants;
- tests cannot be written without unsafe or unrelated tooling;
- the task would create unused scaffolding.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Also run focused tests, adjusted to exact class names if needed:

```bash
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.presentation.WheatGrowthInspectDiagnosticTest --no-daemon
```

If command composition or plugin runtime wiring changes, also run the relevant focused command/plugin tests.

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java
git diff -- java/app/src/test/java
git diff -- java/app/src/pluginTest/java
```

Boundary checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application

git grep -n "io.github.henriquemichelini.dynamicbiomes.crops" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore || true

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/" || true
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether inspect output now reflects current-season-adjusted runtime wheat behavior.
5. Architecture boundary risks.
6. Deferred work.
