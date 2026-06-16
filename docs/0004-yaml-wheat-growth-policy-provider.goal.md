---
id: DB-CROPS-002
type: infrastructure
bounded_context: crops
capability: growth
layer: infrastructure
status: Ready
expected_commit: "feat(crops): add yaml wheat growth policy provider"
---

# Goal Card: Add YAML wheat growth policy provider

## Status

Ready

## Goal

Add a YAML-backed provider that loads configured wheat natural-growth chance policies per supported biome for the crops/growth capability.

## Why Now

The pure domain wheat growth chance policy now exists, but there is no typed configuration adapter that can supply those policies from plugin resources. This is the next narrow step before any Paper `BlockGrowEvent` listener or runtime wiring.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0003-wheat-growth-chance-policy.goal.md` or the actual committed filename for `DB-CROPS-001`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/infrastructure/YamlOreDropPolicyProvider.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/infrastructure/`
- `java/app/src/main/resources/ore-drops.yml`

## Current State

- `crops/growth/domain` contains the pure wheat growth chance decision model.
- `WheatGrowthChance` validates inclusive `[0.0, 1.0]` chances.
- `WheatGrowthChancePolicy` decides allow/cancel through a deterministic variation source.
- There is no YAML-backed crop growth configuration provider.
- There is no `crop-growth.yml` resource.
- There is no Bukkit/Paper listener and no runtime wiring for crop growth.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `infrastructure`, with only the provider port added to `domain` if it does not already exist

Rules:

- Configuration parsing belongs in `crops/growth/infrastructure`.
- The provider port belongs in `crops/growth/domain` only if needed by the YAML adapter.
- The provider may use the published `BiomeId` vocabulary from `biome/identity/domain`.
- Raw YAML, file I/O, SnakeYAML, Bukkit, and Paper must not enter `crops/growth/domain`.
- Do not add runtime listener registration or plugin lifecycle wiring in this slice.

## Acceptance Behavior

1. Given a valid `crop-growth.yml` entry for `minecraft:forest` wheat with `growth-chance: 1.0`, when the provider is asked for the forest wheat policy, then the returned policy allows natural wheat growth.
2. Given a valid `crop-growth.yml` entry for `minecraft:forest` wheat with `growth-chance: 0.0`, when the provider is asked for the forest wheat policy, then the returned policy cancels natural wheat growth.
3. Given a valid `crop-growth.yml` entry with a fractional chance between `0.0` and `1.0`, when the returned policy is evaluated, then it delegates the final allow/cancel decision to the existing deterministic variation source behavior.
4. Given a configured biome that has no wheat growth policy, when the provider is asked for that biome, then it reports unsupported configuration explicitly instead of silently defaulting.
5. Given a wheat growth chance below `0.0` or above `1.0`, when YAML is loaded or the policy is requested, then the existing chance validation rejects it.
6. Given malformed YAML, duplicate biome keys if detectable with the existing YAML approach, invalid namespaced biome IDs, missing required fields, or non-numeric chance values, then the provider fails loudly.
7. Given any non-wheat crop concept, then this slice does not model, parse, or configure it.

## TDD Plan

1. Add the smallest relevant infrastructure tests first for YAML-backed wheat policy loading.
2. Confirm the focused test fails for the expected reason when practical.
3. Add only the domain provider port and explicit unsupported-configuration type if required by the adapter.
4. Implement the YAML provider with the smallest schema needed for wheat growth chance by biome.
5. Add the default `crop-growth.yml` resource only if the provider/resource path requires it.
6. Run focused infrastructure tests.
7. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/UnsupportedWheatGrowthPolicyException.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProvider.java`
- `java/app/src/main/resources/crop-growth.yml`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProviderTest.java`

These are expected files, not permission to create extra scaffolding. If an equivalent provider port or exception already exists, reuse it rather than creating duplicates.

## Responsibility / Collaboration Notes

Class: `WheatGrowthChancePolicyProvider`

Responsibility:

- Provide a configured `WheatGrowthChancePolicy` for a biome.

Collaborators:

- `BiomeId`
- `WheatGrowthChancePolicy`

Must not:

- Parse YAML.
- Read files.
- Import Bukkit/Paper.
- Decide runtime event behavior.

Class: `YamlWheatGrowthChancePolicyProvider`

Responsibility:

- Load `crop-growth.yml` and translate configured YAML values into typed domain objects.

Collaborators:

- `BiomeId`
- `WheatGrowthChance`
- `WheatGrowthChancePolicy`
- `WheatGrowthChancePolicyProvider`

Must not:

- Register listeners.
- Resolve live block biomes.
- Query seasons.
- Apply decisions to Bukkit/Paper events.
- Add crop rules to biome profiles.

## Suggested Minimal YAML Shape

Prefer the smallest clear schema for this slice, unless existing project conventions strongly indicate another shape:

```yaml
biomes:
  minecraft:forest:
    wheat:
      growth-chance: 0.75
```

Rules:

- `biomes` is required.
- Biome keys must be valid namespaced IDs.
- Only `wheat.growth-chance` is in scope.
- Do not add season, fertility, acceleration, tree, animal, or broad crop fields.

## Out of Scope

Explicitly forbidden work:

- Bukkit/Paper `BlockGrowEvent` listener.
- Runtime registration in `pluginruntime`.
- Application service orchestration.
- Biome resolution from a block location.
- Season-based crop adjustments.
- Crop acceleration or manual growth ticking.
- Any crop other than wheat.
- Any tree or animal behavior.
- Database persistence.
- Public API.
- Reload commands.
- Inspect command changes.
- No-edit audit for this slice.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- the prior wheat growth domain classes are absent or materially different from the implementation report;
- the existing YAML provider conventions require a broader shared YAML abstraction;
- implementing this provider would require runtime listener behavior;
- the provider cannot be tested without unsafe Bukkit/Paper event testing;
- the task would create unused scaffolding beyond the provider port required by this adapter;
- the task would require crop-specific fields in biome profiles.

## Verification

Before implementation, run:

```bash
git status --short
git log --oneline -5
cd java && ./gradlew test build --no-daemon
```

Focused test, adjusting the class name only if the final test name differs:

```bash
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.YamlWheatGrowthChancePolicyProviderTest --no-daemon
```

After implementation, run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java
git diff -- java/app/src/main/resources
git diff -- java/app/src/test/java
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Architecture boundary risks.
5. Deferred work.
