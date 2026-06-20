---
id: CROP-PERF-0005A
type: application
bounded_context: crops
capability: performance
layer: application
status: Ready
expected_commit: "feat(crops): compose crop environmental state"
---

# Goal Card: Compose Crop Environmental State

## Goal

Implement application-layer composition of `CropEnvironmentalState` from published biome and season contracts.

## Short Description

Add a small application collaborator that resolves upstream environmental inputs and returns v1 `CropEnvironmentalState`, without loading crop profiles or calculating crop performance.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0002-define-crop-owned-crop-environmental-state-composition.goal.md`
- `docs/implementation/0003a-add-crop-environmental-state-domain.goal.md`
- Existing application services under `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops`
- Matching application tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`

## Affected Layer(s)

- Application: `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`
- Matching application tests only.

## Acceptance Criteria

1. Given supported biome and season data, when environmental state is composed, then humidity and temperature use `clamp01(biomeVariable * seasonVariableFactor)`.
2. Given supported biome data, when environmental state is composed, then soil fertility comes only from `BiomeProfile.Fertility`.
3. Given no upstream contracts for wind speed, rain strength, or solar incidence, when environmental state is composed, then those variables use neutral defaults.
4. Given `UnsupportedBiomeException` or real upstream failures, when environmental state is composed, then they propagate and are not converted to neutral state.

## Expected Tests To Write First

- Application test with in-memory `BiomeResolver`, `CurrentSeasonQuery`, and `SeasonProfileProvider` stubs for humidity/temperature season factors.
- Application test for biome-only soil fertility.
- Application test for neutral wind speed, rain strength, and solar incidence defaults.
- Application test for `UnsupportedBiomeException` propagation.

## OOP / DDD / TDD Guardrails

- Keep composition in application; do not put upstream port calls in domain.
- Consume only published upstream domain contracts from `biome` and `seasons`; never import upstream infrastructure.
- Do not load crop profiles, calculate performance scores, or touch growth/yield services in this card.
- Use in-memory fakes/stubs in tests; no Bukkit, YAML, file I/O, or framework mocks.
- Write the smallest failing application test first, then implement only the environmental composition behavior required by that test.

## Dependencies

- Card 2: Define Crop-Owned CropEnvironmentalState Composition.
- Card 3A: Add Crop Environmental State Domain.

## Out of Scope

- Crop performance profile loading.
- `CropPerformanceService`.
- Missing crop profile neutralization.
- Runtime wiring.
- Growth/yield behavior.

## Stop Conditions

Stop and report if this slice requires YAML parsing, Bukkit/Paper APIs, runtime wiring, crop profile loading, crop performance scoring changes, or changes to growth/yield consumers.
