---
id: CROP-PERF-0003A
type: domain
bounded_context: crops
capability: performance
layer: domain
status: Ready
expected_commit: "feat(crops): add crop environmental state domain"
---

# Goal Card: Add Crop Environmental State Domain

## Goal

Implement the pure domain value objects needed to represent crop environmental state.

## Short Description

Add validated `crops/performance/domain` types for normalized environmental variables and `CropEnvironmentalState`, without adding crop profiles, scoring, services, YAML, or runtime wiring.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0001-reconcile-crop-environmental-design-in-architecture.goal.md`
- `docs/implementation/0002-define-crop-owned-crop-environmental-state-composition.goal.md`
- Existing crop domain tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`

## Affected Layer(s)

- Domain only: `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- Matching domain tests only.

## Acceptance Criteria

1. Given normalized environmental values from `0.0` through `1.0`, when `CropEnvironmentalState` is constructed, then it stores wind speed, rain strength, humidity, temperature, solar incidence, and soil fertility as crop-owned domain values.
2. Given a value below `0.0`, above `1.0`, NaN, or infinity, when a normalized environmental value is constructed, then construction fails explicitly.
3. Given the new domain package, when dependencies are inspected, then it has no Bukkit/Paper, YAML, file I/O, application, infrastructure, runtime, or quality concept dependencies.

## Expected Tests To Write First

- `CropEnvironmentalStateTest` for successful construction with all six variables.
- `CropEnvironmentalStateTest` or a focused value-object test for lower/upper bounds and invalid numeric values.
- A test or existing architecture check confirming domain stays framework-free.

## OOP / DDD / TDD Guardrails

- Model normalized variables as validated value objects; do not pass raw primitive doubles through unrelated layers without validation.
- Keep `CropEnvironmentalState` a crop-owned read model, not a biome, season, weather, YAML, or Bukkit model.
- Use immutable records where they clearly express value-object semantics.
- Write the smallest failing domain test first, then implement only the state/value objects required by that test.

## Dependencies

- Card 1: Reconcile Crop Environmental Design In Architecture.
- Card 2: Define Crop-Owned CropEnvironmentalState Composition.

## Out of Scope

- Crop profile/preferences.
- Performance scoring.
- `CropPerformanceResult`.
- Provider ports or YAML adapters.
- Application services, runtime wiring, growth/yield behavior, or quality concepts.

## Stop Conditions

Stop and report if this slice requires scoring, profile loading, upstream context resolution, YAML parsing, runtime wiring, or changes to existing crop growth/yield behavior.
