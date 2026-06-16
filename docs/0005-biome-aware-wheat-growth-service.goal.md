---
id: DB-CROPS-003
type: application
bounded_context: crops
capability: growth
layer: application
status: Ready
expected_commit: "feat(crops): add biome-aware wheat growth service"
---

# Goal Card: Biome-aware wheat growth service

## Status

Ready

## Goal

Add an application service that decides whether a natural wheat growth attempt should be allowed or cancelled for a block position by resolving the biome and applying the configured wheat growth policy.

## Why Now

The crops/growth domain policy and YAML-backed provider now exist, but no use-case orchestration exists between a block position, biome resolution, and the configured wheat growth policy.

This slice keeps orchestration outside the future Paper listener so the listener can remain a thin Bukkit adapter in a later card.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0003-wheat-growth-chance-policy.goal.md`
- `docs/0004-yaml-wheat-growth-policy-provider.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/UnsupportedWheatGrowthPolicyException.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/resolution/domain/BiomeResolver.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/resolution/domain/UnsupportedBiomeException.java`
- relevant existing application services, especially the ore-drop application service if present

## Current State

- `crops/growth/domain` owns the wheat growth chance value object, deterministic variation port, allow/cancel decision, policy logic, provider port, and unsupported-policy exception.
- `crops/growth/infrastructure` owns the YAML-backed wheat growth policy provider.
- `crop-growth.yml` exists as a resource but is not wired into runtime behavior.
- No crops/growth application service exists yet.
- No Bukkit `BlockGrowEvent` listener or pluginruntime registration exists yet.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `application`

Rules:

- The service may orchestrate published upstream ports and crops/growth domain objects.
- The service must use `BiomeResolver` and `BiomeContext` only through published biome contracts.
- The service must not import Bukkit/Paper, YAML, file I/O, resources, or pluginruntime.
- The service must preserve vanilla behavior for unsupported biome or unsupported wheat growth policy.
- The service must not hide malformed configuration, I/O failures, programming errors, or unrelated runtime failures.

## Acceptance Behavior

1. Given a block position in a supported biome with configured wheat growth chance `1.0`, when the service decides natural wheat growth, then the decision is `ALLOW`.
2. Given a block position in a supported biome with configured wheat growth chance `0.0`, when the service decides natural wheat growth, then the decision is `CANCEL`.
3. Given a block position in a supported biome with a configured intermediate chance, when the deterministic variation source returns a value below the chance threshold, then the decision is `ALLOW`.
4. Given a block position in a supported biome with a configured intermediate chance, when the deterministic variation source returns a value at or above the chance threshold, then the decision is `CANCEL`.
5. Given a block position whose biome cannot be resolved as a supported DynamicBiomes biome, when the service decides natural wheat growth, then the decision is `ALLOW` to preserve vanilla behavior.
6. Given a supported biome with no configured wheat growth policy, when the service decides natural wheat growth, then the decision is `ALLOW` to preserve vanilla behavior.
7. Given a resolver, provider, or policy failure that is not an explicit unsupported-biome or unsupported-policy case, then the failure is not swallowed.

## TDD Plan

1. Add `WheatGrowthServiceTest` with in-memory fakes/stubs for `BiomeResolver`, `WheatGrowthChancePolicyProvider`, and `WheatGrowthChanceVariationSource`.
2. Confirm the focused test fails for the expected reason before production code exists.
3. Implement the smallest application service needed to pass.
4. Run the focused test.
5. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthServiceTest.java`

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `WheatGrowthService`

Responsibility:

- Accept a `BlockPosition` for a natural wheat growth attempt.
- Resolve the block biome through the published `BiomeResolver` port.
- Load the configured wheat growth policy through `WheatGrowthChancePolicyProvider`.
- Delegate allow/cancel logic to `WheatGrowthChancePolicy` and the injected deterministic variation source.
- Convert unsupported biome or unsupported wheat policy into `ALLOW`.

Collaborators:

- `BiomeResolver`
- `WheatGrowthChancePolicyProvider`
- `WheatGrowthChanceVariationSource`
- `WheatGrowthChancePolicy`

Must not:

- Know about Bukkit `BlockGrowEvent`, `Block`, `Material`, `Location`, or `World`.
- Parse YAML or load files.
- Register listeners or touch plugin lifecycle.
- Add crop kinds beyond wheat.
- Add season adjustment or dynamic biome state.

## Out of Scope

Explicitly forbidden work:

- Bukkit/Paper `BlockGrowEvent` listener.
- Runtime listener registration in `pluginruntime`.
- Changes to `plugin.yml`.
- Changes to `crop-growth.yml` unless tests reveal the previous slice left it malformed.
- YAML parsing/provider changes.
- New crop kinds.
- Bonemeal or player-triggered growth handling.
- Season effects.
- Inspect commands or player-facing diagnostics.
- Public API.
- Database persistence.
- No-edit audit.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- the crops/growth domain policy or provider port from previous cards does not exist;
- the required published biome contracts do not exist;
- implementation would require importing Bukkit/Paper into application or domain;
- implementation would require YAML/file I/O in application or domain;
- implementation requires listener registration or runtime behavior;
- tests cannot be written without unsafe Bukkit/Paper tooling;
- the task would create unused scaffolding;
- existing names differ enough that the service cannot be added without broad refactoring.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.application.WheatGrowthServiceTest --no-daemon
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether unsupported biome and unsupported policy preserve vanilla behavior.
5. Architecture boundary risks.
6. Deferred work.
