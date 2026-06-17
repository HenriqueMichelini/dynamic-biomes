---
id: DB-CROPS-007
type: domain
bounded_context: crops
capability: growth
layer: domain
status: Ready
expected_commit: "feat(crops): add wheat seasonal growth factor"
---

# Goal Card: Wheat seasonal growth factor

## Status

Ready

## Goal

Add pure domain support for applying an optional crop-owned seasonal factor to a configured wheat natural growth chance before making the allow/cancel decision.

## Why Now

The first wheat growth feature is implemented, wired, inspected, and manually validated. The next useful behavior is to let crops interpret the already-existing seasons context without changing Bukkit listeners, YAML schema, runtime wiring, or additional crop kinds.

This is justified by the completed wheat pipeline and by the project direction that feature domains interpret environmental context while keeping crop-specific behavior owned by `crops/growth`.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0003-wheat-growth-chance-policy.goal.md`
- `docs/0004-yaml-wheat-growth-policy-provider.goal.md`
- `docs/0005-biome-aware-wheat-growth-service.goal.md`
- `docs/0006-paper-wheat-growth-listener.goal.md`
- `docs/0007-wire-wheat-growth-runtime.goal.md`
- `docs/0008-inspect-wheat-growth-policy.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChance.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChanceVariationSource.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthDecision.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyTest.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain/OreDropSeasonalAdjustment.java`, if present, only to align naming/validation style where appropriate

## Current State

- `WheatGrowthChance` validates inclusive probability values from `0.0` to `1.0`.
- `WheatGrowthChancePolicy` can decide allow/cancel behavior for a configured wheat growth chance using an injected variation source.
- `YamlWheatGrowthChancePolicyProvider` loads the current base wheat chance from `crop-growth.yml`.
- `WheatGrowthService` resolves biome and delegates to the configured wheat policy.
- `PaperWheatGrowthListener` adapts `BlockGrowEvent` to the service and is now runtime-wired.
- `/dynamicbiomes inspect` can report wheat growth policy diagnostics.
- Crop growth does not yet consume current season.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `domain`

Rules:

- This slice may import only pure domain types, plus published upstream season identity if needed, such as `SeasonId`.
- Do not import Bukkit/Paper, YAML, file I/O, pluginruntime, season infrastructure, or biome infrastructure.
- Seasonal growth factors are crop-owned policy data; do not add crop behavior fields to `biome` or `seasons` profiles.
- Preserve existing `WheatGrowthChancePolicy.decide()` behavior when no season is supplied.

## Acceptance Behavior

1. Given a base wheat growth chance of `0.5` and no seasonal factor for the supplied season, when calculating the effective chance, then the result is `0.5`.
2. Given a base wheat growth chance of `0.5` and a seasonal factor of `0.0`, when calculating the effective chance for that season, then the result is `0.0`.
3. Given a base wheat growth chance of `0.5` and a seasonal factor of `1.5`, when calculating the effective chance for that season, then the result is `0.75`.
4. Given a base wheat growth chance of `0.75` and a seasonal factor of `2.0`, when calculating the effective chance for that season, then the result is capped at `1.0`.
5. Given a base wheat growth chance of `0.5` and a seasonal factor below `0.0`, then construction rejects the invalid factor.
6. Given a policy with a seasonal factor, when deciding growth for that season, then the existing deterministic variation source is evaluated against the effective seasonal chance.
7. Given existing callers that use `decide()` without season, behavior remains unchanged and uses the configured base chance.

## TDD Plan

1. Add or update the smallest relevant domain tests first, likely in `WheatGrowthChancePolicyTest` or a new focused test beside it.
2. Confirm the focused test fails for the expected reason when practical.
3. Add only the minimal domain model needed for seasonal factor support.
4. Update `WheatGrowthChancePolicy` only as needed to expose effective seasonal chance and season-aware decision behavior.
5. Run focused domain tests.
6. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthSeasonalFactor.java` or the smallest equivalent value object justified by current naming

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyTest.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthSeasonalFactorTest.java`, only if a separate value object is created

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `WheatGrowthChancePolicy`

Responsibility:

- Own the configured base wheat growth chance.
- Apply an optional crop-owned seasonal factor for a supplied `SeasonId`.
- Decide allow/cancel using the effective chance and the existing deterministic variation source.

Collaborators:

- `WheatGrowthChance`
- `WheatGrowthChanceVariationSource`
- `SeasonId`, if a season-keyed factor map is needed

Must not:

- Query the current season.
- Resolve biomes.
- Read YAML.
- Know about Bukkit events.
- Mutate runtime configuration.

Class: `WheatGrowthSeasonalFactor` or equivalent

Responsibility:

- Represent a validated non-negative multiplier applied to the base configured chance.

Must not:

- Contain season lookup logic.
- Read configuration.
- Import infrastructure.

## Out of Scope

Explicitly forbidden work:

- Do not update `crop-growth.yml`.
- Do not update `YamlWheatGrowthChancePolicyProvider`.
- Do not update `WheatGrowthService` to query `CurrentSeasonQuery`.
- Do not update `PaperWheatGrowthListener`.
- Do not update `DynamicBiomes` runtime wiring.
- Do not update `/dynamicbiomes inspect` output.
- Do not add season effects to `season-profiles.yml`.
- Do not add crop-specific fields to biome profiles.
- Do not add carrots, potatoes, beetroots, trees, animals, acceleration, bonemeal handling, reload commands, public API, or persistence.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- current wheat growth classes differ enough that this card would require a broader refactor;
- implementing this requires YAML parsing, runtime wiring, listener changes, or application service changes;
- implementing this requires importing season infrastructure or arbitrary internal season packages;
- tests cannot be written without Bukkit/Paper, YAML, file I/O, or unrelated tooling;
- the task would create generic multi-crop scaffolding not used by wheat seasonal factors;
- the task would change runtime behavior before YAML/application/runtime slices explicitly opt into seasonal growth.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Also inspect architecture-sensitive imports for the changed domain files:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer|pluginruntime|infrastructure" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether existing no-season wheat behavior remains unchanged.
5. Architecture boundary risks.
6. Deferred work.
