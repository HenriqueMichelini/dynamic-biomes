---
id: DB-CROPS-009
type: application
bounded_context: crops
capability: growth
layer: application
status: Ready
expected_commit: "feat(crops): apply seasonal wheat growth factor"
---

# Goal Card: Apply seasonal wheat growth factor to wheat growth decisions

## Status

Ready

## Goal

Make wheat natural growth decisions use the current configured season when a wheat growth policy has seasonal factors.

## Why Now

The crops/growth domain can calculate season-adjusted wheat growth chances, and the YAML provider can load optional season-keyed factors from `crop-growth.yml`. The runtime still uses the no-season decision path, so configured seasonal factors are currently parsed but not applied.

This card activates that existing capability through the application service and minimal runtime composition only. It does not add new crop behavior, new YAML shape, new listener behavior, or broad balancing.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0009-wheat-seasonal-growth-factor.goal.md`
- `docs/0010-yaml-wheat-seasonal-growth-factors.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthServiceTest.java`
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java`

## Current State

- `WheatGrowthChancePolicy` supports no-season decisions and season-aware decisions.
- `YamlWheatGrowthChancePolicyProvider` loads optional `wheat.seasonal-factors` into crop-owned domain policy.
- `WheatGrowthService` currently resolves biome, loads the configured wheat policy, and delegates to the no-season decision path.
- `DynamicBiomes` wires wheat growth runtime, but seasonal crop factors are not applied at runtime.
- `CurrentSeasonQuery` is an explicitly published seasons contract and may be consumed by downstream feature contexts.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `application`

Rules:

- `crops/growth/application` may depend on crop domain ports, published biome contracts, published season contracts, and shared spatial vocabulary.
- `crops/growth/application` must not import Bukkit/Paper, YAML, file I/O, or pluginruntime.
- `pluginruntime` may import and compose crops, biome, and seasons objects only for startup/composition.
- Do not put crop-specific seasonal behavior in `seasons` or `biome`.
- Do not change the `crop-growth.yml` schema in this card.

## Acceptance Behavior

1. Given a supported biome, a supported wheat growth policy, and current season `minecraft:spring`, when that policy has a spring seasonal factor, then `WheatGrowthService` decides using `policy.decide(currentSeason)` rather than the no-season decision path.
2. Given base chance `0.50`, current season factor `0.00`, and a deterministic variation value above `0.00`, when wheat growth is evaluated, then the service returns `CANCEL_GROWTH`.
3. Given base chance `0.50`, current season factor `2.00`, and any deterministic variation value in the normal unit range, when wheat growth is evaluated, then the service returns `ALLOW_GROWTH` because the effective chance is capped at `1.0` by the domain policy.
4. Given a current season with no configured factor, when wheat growth is evaluated, then the service falls back to the base configured chance.
5. Given an unsupported biome, then wheat growth remains `ALLOW_GROWTH` as vanilla fallback and the season query does not need to be consulted.
6. Given an unsupported wheat growth policy, then wheat growth remains `ALLOW_GROWTH` as vanilla fallback and the season query does not need to be consulted.
7. Given a real failure from the biome resolver, provider, current season query, or policy decision path, then the failure is not swallowed.
8. Runtime composition passes the existing current-season query into the wheat growth service so configured seasonal factors affect actual `PaperWheatGrowthListener` decisions.

## TDD Plan

1. Add or update focused `WheatGrowthServiceTest` cases for season-aware decision behavior first.
2. Confirm the focused tests fail for the expected reason when practical.
3. Update `WheatGrowthService` to consume the published `CurrentSeasonQuery` contract and call the season-aware policy decision path.
4. Update runtime composition in `DynamicBiomes` only as needed to pass the already-existing current-season query into `WheatGrowthService`.
5. Update or add focused plugin runtime tests only if constructor/wiring behavior is observable through existing plugin test support.
6. Run focused tests.
7. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthServiceTest.java`
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java` only if needed to preserve or verify wiring

Documentation:

- `ARCHITECTURE.md` only if the implemented runtime/application behavior described there changes.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `WheatGrowthService`

Responsibility:

- Resolve biome support for a crop growth event position.
- Load the configured wheat growth policy for the resolved biome.
- Read the current season through the published `CurrentSeasonQuery` port.
- Delegate the final season-aware allow/cancel decision to `WheatGrowthChancePolicy`.
- Preserve explicit unsupported biome/policy vanilla fallback behavior.

Collaborators:

- `BiomeResolver`
- `WheatGrowthChancePolicyProvider`
- `CurrentSeasonQuery`
- `WheatGrowthChancePolicy`

Must not:

- Import Bukkit/Paper.
- Parse YAML.
- Read files.
- Own seasonal profile data.
- Catch broad exceptions.
- Implement random/variation rules itself.

Class: `DynamicBiomes`

Responsibility:

- Pass the already-composed current-season query into the wheat growth service.
- Preserve existing listener registration and resource initialization behavior.

Must not:

- Add new commands.
- Add new listeners.
- Add reload behavior.
- Add crop balancing rules.

## Out of Scope

Explicitly forbidden work:

- Changing `crop-growth.yml` schema or default values.
- Changing `season-profiles.yml` or `season-cycle.yml`.
- Adding new crop kinds.
- Adding crop acceleration mechanics.
- Adding bonemeal-specific behavior.
- Adding action bar feedback.
- Changing `/dynamicbiomes inspect` output.
- Adding reload commands.
- Adding public API.
- Adding database persistence.
- Refactoring ore diagnostics beyond what is required to keep tests compiling.
- Introducing a generic crop system or generic effect engine.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- `CurrentSeasonQuery` is not available as a published seasons contract in the current code;
- applying the current season requires changes to `biome`, `seasons`, or YAML schemas;
- runtime wiring cannot access the existing current-season query without broader lifecycle refactoring;
- the implementation would require adding a new listener, command, public API, or reload mechanism;
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

Also inspect architecture-sensitive imports where relevant:

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
4. Whether seasonal factors now affect runtime wheat growth decisions.
5. Architecture boundary risks.
6. Deferred work.
