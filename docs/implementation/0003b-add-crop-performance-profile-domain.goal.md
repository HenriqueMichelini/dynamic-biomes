---
id: CROP-PERF-0003B
type: domain
bounded_context: crops
capability: performance
layer: domain
status: Ready
expected_commit: "feat(crops): add crop performance profile domain"
---

# Goal Card: Add Crop Performance Profile Domain

## Goal

Implement the pure domain model for crop performance preference profiles.

## Short Description

Add validated crop-owned profile/preference types that describe a crop's preferred environmental conditions, without calculating performance scores or loading YAML.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0003a-add-crop-environmental-state-domain.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/identity/domain`
- Existing crop domain tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`

## Affected Layer(s)

- Domain only: `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- Matching domain tests only.

## Acceptance Criteria

1. Given a supported `CropKind` and valid preferred environmental values, when a crop performance profile is constructed, then it represents the crop-owned preferences for the six `CropEnvironmentalState` variables.
2. Given invalid or missing profile preference values, when a profile is constructed, then construction fails explicitly through domain validation.
3. Given the profile domain model, when code is inspected, then it contains no scoring, result calculation, YAML parsing, Bukkit/Paper types, runtime wiring, or quality concepts.

## Expected Tests To Write First

- `CropPerformanceProfileTest` for successful construction of a crop profile.
- `CropPerformanceProfileTest` for invalid or missing preference values.
- A negative test or architecture check confirming the profile type has no framework dependencies.

## OOP / DDD / TDD Guardrails

- Keep profile data as validated domain value objects, not raw maps or configuration DTOs.
- Reuse the normalized value object from Card 3A instead of creating duplicate validation.
- Keep behavior limited to invariants needed to represent preferences; do not add scoring methods in this card.
- Write the smallest failing profile test first, then implement only the profile model required by that test.

## Dependencies

- Card 3A: Add Crop Environmental State Domain.

## Out of Scope

- Performance scoring.
- `CropPerformanceResult`.
- Provider ports or YAML adapters.
- Application services, runtime wiring, growth/yield behavior, or quality concepts.

## Stop Conditions

Stop and report if this slice requires scoring formulas, provider ports, YAML parsing, application orchestration, runtime wiring, or changes to existing crop growth/yield behavior.
