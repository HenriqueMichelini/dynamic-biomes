---
id: DB-CROPS-008
type: infrastructure
bounded_context: crops
capability: growth
layer: infrastructure
status: Ready
expected_commit: "feat(crops): load wheat seasonal growth factors"
---

# Goal Card: Load Wheat Seasonal Growth Factors From YAML

## Status

Ready

## Goal

Extend the YAML-backed wheat growth policy provider so `crop-growth.yml` can configure optional season-keyed wheat growth factors for a biome policy.

## Why Now

The domain now supports optional `SeasonId` keyed wheat growth factors, but the configured YAML provider still needs to translate resource data into that existing domain model.

This is the next smallest slice before application/runtime seasonal behavior because it keeps parsing in infrastructure and preserves current runtime behavior.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0009-wheat-seasonal-growth-factor.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthSeasonalFactor.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProvider.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProviderTest.java`
- `java/app/src/main/resources/crop-growth.yml`
- `java/app/src/main/resources/season-profiles.yml`

## Current State

- `WheatGrowthChancePolicy` supports a configured base chance and optional season-keyed growth factors.
- `WheatGrowthSeasonalFactor` validates factor values in domain.
- `YamlWheatGrowthChancePolicyProvider` already loads `biomes.<biome>.wheat.growth-chance`.
- Runtime wheat growth is wired, but it still uses no-season decisions and must remain behaviorally unchanged in this slice.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `infrastructure`

Rules:

- YAML parsing and file/resource handling stay in `crops/growth/infrastructure`.
- Raw YAML must not leak into domain or application.
- Use the existing `SeasonId` published language; do not import seasons infrastructure.
- Do not make `biome`, `seasons`, or `ore` depend on `crops`.
- Do not add runtime wiring or listener behavior in this card.

## Acceptance Behavior

1. Given an existing valid wheat policy YAML without `seasonal-factors`, when the provider loads it, then the policy uses the configured base chance and behaves exactly as before for no-season decisions.
2. Given a wheat policy YAML with `seasonal-factors`, when the provider loads it, then each YAML key is translated to a `SeasonId` and each value is translated to a `WheatGrowthSeasonalFactor`.
3. Given a loaded policy with a seasonal factor for a season, when `effectiveChanceFor(seasonId)` is called, then the returned effective chance reflects the configured base chance adjusted by that factor according to the existing domain rules.
4. Given a loaded policy without a factor for a requested season, when `effectiveChanceFor(seasonId)` is called, then the base chance is used according to the existing domain rules.
5. Given a negative seasonal factor, when the provider loads the YAML, then loading fails instead of silently normalizing or ignoring the value.
6. Given duplicate keys inside `seasonal-factors`, when the provider loads the YAML, then loading fails consistently with existing duplicate-key behavior.
7. Given unsupported or missing wheat policy data, existing unsupported-policy behavior remains unchanged.

## TDD Plan

1. Add or update `YamlWheatGrowthChancePolicyProviderTest` first.
2. Cover no-factors backward compatibility before implementing seasonal factor parsing.
3. Cover one valid seasonal factor and one missing-season fallback.
4. Cover invalid negative factor and duplicate seasonal factor keys.
5. Confirm the focused provider test fails for the expected reason when practical.
6. Implement only the YAML-provider/resource changes needed to pass.
7. Run focused tests.
8. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProvider.java`
- `java/app/src/main/resources/crop-growth.yml`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProviderTest.java`

Documentation:

- `ARCHITECTURE.md` only if its implemented/deferred behavior section becomes inaccurate after this change.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `YamlWheatGrowthChancePolicyProvider`

Responsibility:

- Parse crop-owned YAML configuration into typed wheat growth domain objects.
- Preserve existing unsupported-policy and invalid-configuration behavior.

Collaborators:

- `BiomeId`
- `SeasonId`
- `WheatGrowthChance`
- `WheatGrowthSeasonalFactor`
- `WheatGrowthChancePolicy`

Must not:

- Query the current season.
- Resolve biomes from Bukkit blocks or locations.
- Decide whether a crop event is allowed or cancelled.
- Register listeners.
- Read `season-profiles.yml` beyond optionally inspecting it to choose valid example IDs for the default resource.

## Suggested YAML Shape

Prefer this shape unless the existing provider/resource convention clearly requires a different local naming style:

```yaml
biomes:
  minecraft:forest:
    wheat:
      growth-chance: 0.75
      seasonal-factors:
        spring: 1.20
        summer: 1.00
        autumn: 0.80
        winter: 0.50
```

Before updating the default resource, inspect `season-profiles.yml` and use the actual configured season IDs. If the repository uses different names, preserve those names.

## Out of Scope

Explicitly forbidden work:

- Do not change `WheatGrowthService` to query `CurrentSeasonQuery`.
- Do not change `PaperWheatGrowthListener`.
- Do not change `DynamicBiomes` runtime composition.
- Do not change `/dynamicbiomes inspect` output.
- Do not add crop kinds beyond wheat.
- Do not add season profile fields for crop behavior.
- Do not add acceleration mechanics.
- Do not add bonemeal/player-triggered behavior.
- Do not add reload commands, public API, database persistence, or dynamic biome state.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- `WheatGrowthChancePolicy` does not expose a safe existing way to construct or observe policies with seasonal factors;
- adding this feature would require changes outside the provider/resource/test scope, except for documentation alignment;
- implementation requires importing seasons infrastructure;
- implementation requires changing runtime behavior;
- tests cannot be written without introducing unsafe or unrelated tooling;
- the task would create unused scaffolding.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.YamlWheatGrowthChancePolicyProviderTest --no-daemon
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
git diff -- java/app/src/main/resources/crop-growth.yml
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
```

Architecture-sensitive checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/"
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. YAML shape implemented.
4. Commands run and results.
5. Whether runtime wheat behavior remained unchanged.
6. Architecture boundary risks.
7. Deferred work.
