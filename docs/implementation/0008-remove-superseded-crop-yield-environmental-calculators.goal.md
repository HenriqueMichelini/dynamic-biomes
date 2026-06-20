---
id: CROP-PERF-0008
type: domain
bounded_context: crops
capability: yield
layer: domain
status: Ready
expected_commit: "refactor(crops): remove superseded yield environmental calculators"
---

# Goal Card: Remove Superseded Crop-Yield Environmental Calculators

## Goal

Remove or retire yield-specific environmental calculators after crop performance owns environmental interpretation.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0007-make-crop-yield-use-crop-performance.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance`
- Matching yield and crop-performance tests under `java/app/src/test/java`

## Current-State Problem

`CropYieldClimateFactorCalculator`, `CropYieldBiomeFactorCalculator`, and `CropYieldEnvironmentalFactorCalculator` duplicate or conflict with `crops/performance`.

## Target-State Outcome

Yield no longer owns environmental interpretation; it consumes crop performance factors.

Required domain decisions:

- `crops/performance` owns crop-specific interpretation.
- Biome-scoped crop yield multipliers are transitional once crop performance exists.
- Yield must not double-apply season climate.
- Once yield consumes `CropPerformanceResult`, do not apply `CropYieldClimateFactorCalculator`.
- No quality tier, quality probability, quality roll, item quality, quality diagnostics, or `/dynamicbiomes inspect` quality output.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- `ARCHITECTURE.md`

## OOP / DDD / TDD Guardrails

- Remove only responsibilities that are now owned by `crops/performance`; do not move environmental interpretation back into `crops/yield`.
- Keep remaining yield classes focused on yield policy, seasonal yield tuning, and final quantity calculation.
- Do not leave passive compatibility wrappers or empty classes/packages after references are removed.
- Preserve or replace behavioral coverage before deleting tests; crop-performance tests must cover any transferred environmental interpretation.
- Use TDD-style cleanup: first add or update safety tests that prove the active yield formula no longer uses superseded calculators, then remove dead code.

## Implementation Boundaries

- Cleanup only after Card 7.
- Do not change formulas beyond removing dead transitional code.
- Do not delete coverage unless equivalent `crops/performance` tests exist.
- Do not remove biome-scoped yield policy unless a separate card explicitly replaces it.
- Do not add quality output.

## Test/Verification Expectations

- Follow TDD for any replacement or deletion safety checks.
- Run `rg` for removed class references.
- Run focused yield and crop performance tests.
- Run `git diff --check`.
- Run `cd java && ./gradlew test build --no-daemon`.

## Dependencies

- Card 6: Make Crop Growth Use Crop Performance.
- Card 7: Make Crop Yield Use Crop Performance.

## Risks Or Migration Notes

- Stop if any calculator remains wired into active runtime after Card 7.
- Keep cleanup scoped to superseded environmental interpretation code; do not use this card for broader yield policy redesign.

## Acceptance Behavior

1. Given Card 7 is complete, when cleanup is done, then yield no longer owns or applies superseded environmental interpretation calculators.
2. Given references are searched, when cleanup is done, then removed calculators have no active runtime references.
3. Given crop performance tests exist, when old calculator tests are removed or changed, then equivalent behavior is covered under `crops/performance`.

## Stop Conditions

Stop and report if Card 7 is not complete, if any calculator still appears in active runtime composition, or if equivalent crop performance coverage does not exist.
