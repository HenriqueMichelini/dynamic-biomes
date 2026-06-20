---
id: CROP-PERF-0003
type: domain
bounded_context: crops
capability: performance
layer: domain
status: Ready
expected_commit: "feat(crops): add crop performance domain model"
---

# Goal Card: Add Pure Crop Performance Domain Model

## Goal

Implement pure crop performance domain types and scoring.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0001-reconcile-crop-environmental-design-in-architecture.goal.md`
- `docs/implementation/0002-define-crop-owned-crop-environmental-state-composition.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`

## Current-State Problem

Growth and yield own separate factors, and no crop model compares current environmental state to crop preferences.

## Target-State Outcome

`crops/performance/domain` calculates `CropPerformanceResult` from a crop profile and `CropEnvironmentalState`.

Required domain decisions:

- `crops/performance` owns crop-specific interpretation.
- `CropEnvironmentalState` is a crop-side read model of normalized `0.0..1.0` variables.
- Variables are wind speed, rain strength, humidity, temperature, solar incidence, and soil fertility.
- `CropPerformanceResult` exposes only optional `overallScore`, `growthSpeedFactor`, `growthChanceFactor`, and `harvestQuantityFactor`.
- No quality tier, quality probability, quality roll, item quality, quality diagnostics, or `/dynamicbiomes inspect` quality output.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`

## OOP / DDD / TDD Guardrails

- Keep all behavior in `crops/performance/domain`; no application, infrastructure, runtime, Bukkit/Paper, YAML, or file I/O dependencies.
- Use value objects for normalized variables and profile preferences; validate construction invariants at object boundaries.
- Prefer immutable records for simple value objects and explicit classes/domain services for behavior that compares profiles to environmental state.
- Keep scoring behavior close to the model that owns the data; do not create passive data bags plus unrelated procedural helpers unless a pure domain service is the clearest responsibility.
- Add or update the smallest matching domain tests first under the mirrored test package, confirm expected failure when practical, then implement.

## Implementation Boundaries

- Domain only.
- No Bukkit, Paper, YAML, file I/O, services, providers, listeners, runtime wiring, commands, or quality concepts.
- Do not modify `crop-growth.yml`, `crop-yields.yml`, or introduce `crop-profiles.yml` in this card.
- Do not change existing crop growth or crop yield behavior.
- Do not create empty packages or unused scaffolding.

## Test/Verification Expectations

- Follow TDD: add the smallest pure domain tests first and confirm they fail for the expected reason when practical.
- Cover normalized value validation.
- Cover exact preference match.
- Cover poor match.
- Cover derived factors.
- Cover optional `overallScore`.
- Cover absence of quality fields or quality concepts.
- Run focused domain tests, then `cd java && ./gradlew test build --no-daemon`.
- Run `git diff --check`.

## Dependencies

- Card 1: Reconcile Crop Environmental Design In Architecture.
- Card 2: Define Crop-Owned CropEnvironmentalState Composition.

## Risks Or Migration Notes

- `growthSpeedFactor` exists immediately even though runtime growth-speed wiring remains deferred.
- Keep the scoring model pure and deterministic enough for domain tests.

## Acceptance Behavior

1. Given a crop profile and matching `CropEnvironmentalState`, when performance is calculated, then the result exposes the expected optional score and neutral-or-positive factors according to the domain model.
2. Given invalid normalized values, when domain objects are constructed, then construction fails explicitly.
3. Given the public result type, when inspected by tests, then it exposes only `overallScore`, `growthSpeedFactor`, `growthChanceFactor`, and `harvestQuantityFactor`.

## Stop Conditions

Stop and report if the domain model requires framework APIs, YAML parsing, runtime composition, quality concepts, or changes to existing growth/yield behavior.
