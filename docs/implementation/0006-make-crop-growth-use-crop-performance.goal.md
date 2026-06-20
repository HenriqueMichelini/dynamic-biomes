---
id: CROP-PERF-0006
type: application
bounded_context: crops
capability: growth
layer: application
status: Ready
expected_commit: "feat(crops): apply crop performance to growth chance"
---

# Goal Card: Make Crop Growth Use Crop Performance

## Goal

Apply crop performance to existing DynamicBiomes crop growth behavior.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0005-add-crop-performance-application-service.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`

## Current-State Problem

Growth uses configured biome/crop chance and legacy crop growth seasonal factor only.

## Target-State Outcome

Existing growth chance remains active and is multiplied by `cropPerformance.growthChanceFactor`; `growthSpeedFactor` remains available but not wired to runtime speed behavior.

Required domain decisions:

- Missing crop profile / unsupported crop-performance profile becomes neutral `CropPerformanceResult`.
- Unsupported biome remains an unsupported upstream/environment condition so growth consumers preserve vanilla behavior.
- Runtime growth-speed wiring is deferred.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application`
- Possibly `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application`
- Existing crop growth infrastructure tests if constructor wiring changes require updates.

## OOP / DDD / TDD Guardrails

- Keep crop growth policy rules in `crops/growth/domain`; keep crop performance scoring in `crops/performance/domain`; use the growth application service only to orchestrate the combined result.
- Depend on the crop performance application service or an explicit port-style collaborator; do not duplicate crop-performance calculations inside growth.
- Pluginruntime changes are allowed only for constructor/composition wiring required by this card.
- Do not import Bukkit/Paper events into application or domain packages.
- Add focused growth application tests first for neutral, non-neutral, and fallback behavior before changing production wiring.

## Implementation Boundaries

- Do not change `crop-growth.yml`.
- Do not change listener semantics.
- Do not implement growth-speed runtime wiring.
- Do not add quality output.
- Preserve vanilla growth fallback for unsupported biome or unsupported crop growth policy.
- Do not remove existing crop growth policy behavior.

## Test/Verification Expectations

- Follow TDD in the smallest affected growth service tests.
- Test neutral performance preserving current DynamicBiomes growth behavior.
- Test non-neutral `growthChanceFactor` affecting growth decisions.
- Test missing crop performance preserving current DynamicBiomes behavior through neutral performance.
- Test unsupported biome or unsupported crop growth policy preserving vanilla growth.
- Run focused growth tests, then `cd java && ./gradlew test build --no-daemon`.
- Run `git diff --check`.

## Dependencies

- Card 5: Add Crop Performance Application Service.

## Risks Or Migration Notes

- This is the first explicit crop growth semantic change.
- `growthSpeedFactor` must remain unused in runtime behavior until a separate card explicitly wires growth speed.

## Acceptance Behavior

1. Given neutral crop performance, when growth is evaluated, then current DynamicBiomes growth behavior is preserved.
2. Given non-neutral crop performance, when growth is evaluated, then the existing configured chance is multiplied by `cropPerformance.growthChanceFactor`.
3. Given unsupported biome or unsupported crop growth policy, when growth is evaluated, then vanilla growth behavior is preserved.

## Stop Conditions

Stop and report if the change requires rewriting listener semantics, changing YAML shape, wiring runtime growth speed, or changing unrelated crop yield behavior.
