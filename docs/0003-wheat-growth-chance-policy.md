---
id: DB-CROPS-001
type: domain
bounded_context: crops
capability: growth
layer: domain
status: Ready
expected_commit: "feat(crops): add wheat growth chance policy"
---

# Goal Card: Wheat Growth Chance Policy

## Status

Ready

## Goal

Add pure domain decision logic for whether natural wheat growth is allowed or cancelled based on an already-selected configured chance.

## Why Now

The ore-drop work is considered closed for now, and the next smallest feature slice is crop growth behavior for one crop type with no Bukkit listener, YAML provider, runtime wiring, or broad balancing.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- Existing goal cards under `docs/`, if any, before choosing the final file number
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops`, if it exists
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`, if it exists
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain`

## Current State

- Crop growth behavior is not yet implemented.
- This card starts the `crops/growth/domain` capability only if equivalent domain classes do not already exist.
- Ore-drop behavior must not be changed by this slice.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `domain`

Rules:

- Keep this slice pure domain code: no Bukkit/Paper, YAML, file I/O, database, scheduler, command, or plugin runtime code.
- Do not add crop-specific fields or rules to `biome` or `seasons`.
- Do not add generic crop scaffolding beyond what is required to decide natural wheat growth.
- Biome-specific policy lookup is out of scope; this domain slice receives an already-selected wheat growth chance.

## Acceptance Behavior

1. Given wheat growth chance `1.0`, when natural wheat growth is evaluated, then growth is allowed.
2. Given wheat growth chance `0.0`, when natural wheat growth is evaluated, then growth is cancelled.
3. Given wheat growth chance between `0.0` and `1.0`, when the injected deterministic variation value is lower than the chance, then growth is allowed.
4. Given wheat growth chance between `0.0` and `1.0`, when the injected deterministic variation value is equal to or greater than the chance, then growth is cancelled.
5. Given wheat growth chance below `0.0` or above `1.0`, construction rejects it.
6. Given this domain slice, non-wheat crops are not modeled and no Bukkit `BlockGrowEvent` handling is added.

## TDD Plan

1. Add the smallest domain test for chance boundary validation and decision behavior.
2. Confirm the focused test fails for the expected missing class or missing behavior when practical.
3. Implement only the required domain value object, decision result, policy/service, and deterministic variation port.
4. Run the focused domain tests.
5. Run the full verification command.

Suggested focused command:

```bash
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.WheatGrowthChancePolicyTest --no-daemon
```

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChance.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthDecision.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChanceVariationSource.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicy.java`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChanceTest.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyTest.java`

These are expected files, not permission to create extra scaffolding. Reuse existing equivalent files if they already exist.

## Responsibility / Collaboration Notes

Class: `WheatGrowthChance`

Responsibility:

- Represent a configured wheat growth allow chance in the inclusive range `0.0` to `1.0`.

Must not:

- Know about Bukkit materials, YAML keys, biome IDs, seasons, or runtime configuration files.

Class: `WheatGrowthChanceVariationSource`

Responsibility:

- Provide a deterministic-testable variation value for probabilistic decisions.

Must not:

- Depend on Bukkit, Java file I/O, YAML, or plugin lifecycle APIs.

Class: `WheatGrowthChancePolicy`

Responsibility:

- Decide whether natural wheat growth is allowed or cancelled from a `WheatGrowthChance` and variation value.

Must not:

- Resolve biomes, read configuration, listen for events, mutate world state, or accelerate growth.

## Out of Scope

Explicitly forbidden work:

- Bukkit/Paper `BlockGrowEvent` listener.
- YAML-backed `crop-growth.yml` or any configuration resource.
- Runtime registration or wiring in `pluginruntime`.
- Crop behavior in `biome` profiles.
- Season effects on crop growth.
- Additional crop kinds beyond wheat.
- Tree, animal, ore, database, command, inspect, reload, or public API changes.
- Accelerating growth ticks or manually growing crops.
- Broad balancing or biome-specific lookup behavior.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- an existing crops growth model already implements this behavior and the card would duplicate it;
- implementation requires Bukkit/Paper, YAML, file I/O, database, listener, command, or runtime wiring code;
- implementation requires changing `biome`, `seasons`, `ore`, or `pluginruntime`;
- tests cannot be written as pure domain tests;
- the task would create unused scaffolding;
- the task would require runtime behavior not explicitly included in this card.

Do not stop merely because the `crops/growth/domain` package does not exist; creating only the domain files required by this card is allowed.

## Verification

Before editing, run:

```bash
git status --short
git log --oneline -5
cd java && ./gradlew test build --no-daemon
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
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Architecture boundary risks.
5. Deferred work.
