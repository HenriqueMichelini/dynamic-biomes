---
id: CROP-PERF-0003C
type: domain
bounded_context: crops
capability: performance
layer: domain
status: Ready
expected_commit: "feat(crops): score crop environmental performance"
---

# Goal Card: Add Crop Performance Scoring Domain

## Goal

Implement pure crop performance scoring from a crop profile and crop environmental state.

## Short Description

Add `CropPerformanceResult` and the domain behavior that calculates it from a `CropPerformanceProfile` and `CropEnvironmentalState`, without adding providers, services, YAML, runtime wiring, or growth/yield consumers.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0003a-add-crop-environmental-state-domain.goal.md`
- `docs/implementation/0003b-add-crop-performance-profile-domain.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- Matching domain tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`

## Affected Layer(s)

- Domain only: `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- Matching domain tests only.

## Acceptance Criteria

1. Given a crop profile and matching `CropEnvironmentalState`, when performance is calculated, then the result exposes the expected optional score and neutral-or-positive factors according to the domain model.
2. Given a poor environmental match, when performance is calculated, then the result reflects reduced performance factors according to the domain model.
3. Given the public result type, when inspected by tests, then it exposes only `overallScore`, `growthSpeedFactor`, `growthChanceFactor`, and `harvestQuantityFactor`.
4. Given the scoring domain, when dependencies are inspected, then there are no Bukkit/Paper, YAML, file I/O, application, infrastructure, runtime, or quality concept dependencies.

## Expected Tests To Write First

- `CropPerformanceCalculatorTest` or equivalent for exact preference match.
- `CropPerformanceCalculatorTest` or equivalent for poor match.
- `CropPerformanceResultTest` for exposed factors and optional `overallScore`.
- A test or existing architecture check confirming absence of quality fields/concepts.

## OOP / DDD / TDD Guardrails

- Keep scoring behavior in the crop-performance domain; do not duplicate it in growth, yield, application services, or infrastructure.
- Keep `CropPerformanceResult` immutable and limited to score/factor output.
- Use a pure domain service only if the scoring behavior does not naturally belong on the profile object.
- Write the smallest failing scoring test first, then implement only the scoring/result behavior required by that test.

## Dependencies

- Card 3A: Add Crop Environmental State Domain.
- Card 3B: Add Crop Performance Profile Domain.

## Out of Scope

- Provider ports or YAML adapters.
- Environmental state composition from biome/season contracts.
- Application services, runtime wiring, growth/yield behavior, or quality concepts.

## Stop Conditions

Stop and report if this slice requires YAML parsing, upstream context resolution, runtime composition, quality concepts, or changes to existing crop growth/yield behavior.
