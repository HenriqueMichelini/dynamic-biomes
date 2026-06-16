---
id: DB-CROPS-004
type: infrastructure
bounded_context: crops
capability: growth
layer: infrastructure
status: Ready
expected_commit: "feat(crops): add paper wheat growth listener"
---

# Goal Card: Paper wheat growth listener

## Status

Ready

## Goal

Add a Paper/Bukkit `BlockGrowEvent` listener that applies the existing wheat growth service to natural wheat growth events and cancels the event only when the service returns a cancel decision.

## Why Now

The crops/growth domain policy, YAML-backed policy provider, and application service now exist. The next smallest runtime-facing slice is the Bukkit adapter that translates a natural wheat growth event into a `BlockPosition` and delegates all decision-making to `WheatGrowthService`.

This card intentionally stops before pluginruntime registration so the listener can be implemented and tested as an isolated infrastructure adapter first.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0003-wheat-growth-chance-policy.goal.md`
- `docs/0004-yaml-wheat-growth-policy-provider.goal.md`
- `docs/0005-biome-aware-wheat-growth-service.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthDecision.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/spatial/domain/BlockPosition.java`
- Existing Bukkit/Paper infrastructure listeners, especially ore listeners that translate Bukkit block/world/location data into spatial value objects.
- Existing MockBukkit or listener tests, if any, to match project test style.

## Current State

- `crops/growth/domain` owns wheat growth chance validation, policy decisions, deterministic variation, provider port, and unsupported-policy behavior.
- `crops/growth/infrastructure` owns the YAML-backed wheat growth policy provider and `crop-growth.yml` exists as a packaged resource.
- `crops/growth/application/WheatGrowthService` resolves the biome, loads the configured policy, delegates decision logic, and preserves vanilla behavior for unsupported biome or unsupported wheat policy.
- No Bukkit `BlockGrowEvent` listener exists for crops/growth.
- No crops/growth listener is registered in `pluginruntime`.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `infrastructure`

Rules:

- Bukkit/Paper imports are allowed only in this infrastructure adapter and its infrastructure tests.
- The listener must delegate decision-making to `WheatGrowthService`; it must not duplicate chance, biome, YAML, or policy logic.
- Bukkit `World`, `Block`, `Location`, `Material`, and `BlockGrowEvent` must be translated at the infrastructure boundary.
- Domain and application packages must remain free of Bukkit/Paper, YAML, file I/O, and pluginruntime imports.
- Runtime listener registration is not part of this card.

## Acceptance Behavior

1. Given a natural growth event for `Material.WHEAT` and `WheatGrowthService` returns `ALLOW`, when the listener handles the event, then the event is not cancelled by DynamicBiomes.
2. Given a natural growth event for `Material.WHEAT` and `WheatGrowthService` returns `CANCEL`, when the listener handles the event, then the event is cancelled.
3. Given a natural growth event for a non-wheat material, when the listener handles the event, then the service is not called and the event cancellation state is unchanged.
4. Given an event that is already cancelled before this listener handles it, when the listener is invoked directly in a test, then the service is not called and the event remains cancelled.
5. Given a wheat growth event, when converting the event block to domain input, then the listener uses the existing project pattern for creating `BlockPosition` from the Bukkit world/block coordinates.
6. Given service failures other than explicit unsupported cases already handled inside `WheatGrowthService`, then the listener does not swallow the failure.

## TDD Plan

1. Add or update the smallest infrastructure listener test first.
2. Prefer the existing project listener-test style. Use MockBukkit only if it is already safe and consistent with current tests.
3. Cover wheat allow, wheat cancel, non-wheat ignored, and already-cancelled ignored behavior.
4. Confirm the focused test fails for the expected reason before production code exists.
5. Implement the smallest listener needed to pass.
6. Run focused tests.
7. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperWheatGrowthListener.java`

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperWheatGrowthListenerTest.java`

Documentation, only if required by `AGENTS.md` because structure or runtime documentation changes:

- `ARCHITECTURE.md`
- `AGENTS.md`

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `PaperWheatGrowthListener`

Responsibility:

- Listen for Bukkit/Paper `BlockGrowEvent`.
- Ignore already-cancelled events.
- Ignore non-wheat growth events.
- Convert the event block location to `BlockPosition` using the current project convention.
- Call `WheatGrowthService` for wheat natural growth decisions.
- Cancel the event when the service returns the cancel decision.
- Leave the event uncancelled when the service returns the allow decision.

Collaborators:

- `WheatGrowthService`
- `WheatGrowthDecision`
- `BlockPosition`
- Bukkit/Paper `BlockGrowEvent`

Must not:

- Parse YAML or load `crop-growth.yml`.
- Resolve biomes directly.
- Read season state.
- Perform chance/variation calculations directly.
- Register itself with the plugin manager.
- Send player messages, action bars, sounds, logs, or diagnostics.
- Add broad crop material support beyond wheat.

## Out of Scope

Explicitly forbidden work:

- Runtime registration in `pluginruntime`.
- `plugin.yml` changes.
- YAML provider changes.
- `crop-growth.yml` changes.
- New crop kinds.
- Bonemeal/player-triggered growth handling.
- Growth acceleration or scheduled crop ticks.
- Season effects.
- Inspect commands or player-facing diagnostics.
- Public API.
- Database persistence.
- No-edit audit.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- `WheatGrowthService` or `WheatGrowthDecision` does not exist;
- implementing the listener would require domain or application packages to import Bukkit/Paper;
- safe listener testing is not possible with the current test setup and no smaller adapter-level test can be written;
- the listener cannot convert Bukkit world/block coordinates to `BlockPosition` without introducing a broad shared mapper;
- the implementation requires runtime registration or plugin lifecycle changes;
- the task would create unused scaffolding;
- existing naming differs enough that this listener cannot be added without broad refactoring.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests io.github.henriquemichelini.dynamicbiomes.crops.growth.infrastructure.PaperWheatGrowthListenerTest --no-daemon
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth
git grep -n "pluginruntime" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops || true
git grep -nE "org\.bukkit|io\.papermc" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application || true
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether wheat allow, wheat cancel, non-wheat ignored, and already-cancelled ignored behavior are covered.
5. Whether runtime registration remains deferred.
6. Architecture boundary risks.
7. Deferred work.
