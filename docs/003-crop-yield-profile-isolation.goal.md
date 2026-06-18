---
id: CROP-YIELD-CHARACTERIZATION
type: application
bounded_context: crops
capability: yield
layer: application
status: Ready
expected_commit: "test(crops): characterize current crop yield profile isolation"
---

# Goal Card: Characterize Current Crop Yield Profile Isolation

## Status

Ready

## Goal

Add tests documenting that crop harvest yield currently uses `crop-yields.yml` policy data and crop-owned seasonal factors only, while ignoring `season-profiles.yml` and biome profile climate/environment values.

## Why Now

Crop yield is about to evolve toward an environmental formula using crop config, biome data, and season profile data. Before changing the formula, the current behavior must be frozen with tests so future changes are intentional and reviewable.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicy.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldCropRule.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldMultiplierCalculator.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldQuantityCalculator.java`
- existing tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/`

## Current State

- `PaperCropHarvestListener` maps Bukkit crop material to `CropKind`.
- It counts vanilla produce drops only.
- It calls `CropYieldService.calculateProduceQuantity(...)`.
- It replaces only produce drops when the adjusted quantity differs.
- `CropYieldService` resolves biome through `BiomeResolver`.
- `CropYieldService` loads a configured crop yield policy through `CropYieldPolicyProvider`.
- `CropYieldService` reads current season through `CurrentSeasonQuery`.
- The final quantity is based on:
  - configured crop yield multiplier range;
  - crop-owned seasonal factor from `crop-yields.yml`;
  - probabilistic rounding.
- `SeasonProfileProvider` and `BiomeProfileProvider` are not currently consumed by crop yield runtime behavior.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `yield`
- Layer: `application` tests, possibly existing domain tests if needed

Rules:

- Do not modify production behavior.
- Do not inject `SeasonProfileProvider`.
- Do not inject `BiomeProfileProvider`.
- Do not add a new environmental factor calculator.
- Do not change `crop-yields.yml`.
- Do not change runtime wiring.
- Do not introduce YAML, Bukkit, Paper, or file I/O into domain tests.
- Application tests must use in-memory fakes/stubs for ports.

## Acceptance Behavior

1. Given a configured crop yield multiplier and a current season with a configured crop-owned seasonal factor, when `CropYieldService` calculates produce quantity, then the result uses the crop-owned seasonal factor from the crop yield policy.

2. Given different season profile climate-adjustment data exists somewhere in the project model, when `CropYieldService` calculates produce quantity, then the result is unchanged because `CropYieldService` does not consume `SeasonProfileProvider`.

3. Given different biome profile climate/environment values exist somewhere in the project model, when `CropYieldService` calculates produce quantity, then the result is unchanged because crop yield currently uses only the resolved `BiomeId` to select the configured crop yield policy.

4. Given unsupported biome resolution or unsupported crop yield policy, when `CropYieldService` calculates produce quantity, then vanilla produce quantity is preserved.

## TDD Plan

1. Inspect existing crop yield tests.
2. Add the smallest characterization test in the existing `crops/yield/application` test package.
3. Use deterministic multiplier and quantity variation sources where available.
4. Use in-memory fakes/stubs for:
   - `BiomeResolver`
   - `CropYieldPolicyProvider`
   - `CurrentSeasonQuery`
5. Confirm the tests pass against current behavior.
6. Do not change production code unless an existing missing test seam makes the characterization impossible.
7. Run focused tests, then full verification.

## Expected Files

Production:

- No production file changes expected.

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldServiceTest.java`

Documentation:

- No documentation changes expected unless the current repository state contradicts `ARCHITECTURE.md`.

## Responsibility / Collaboration Notes

Class: `CropYieldService`

Responsibility:

- Resolve biome.
- Load crop yield policy.
- Read current season ID.
- Delegate multiplier and quantity calculation to crop yield domain objects.
- Preserve vanilla quantity for explicit unsupported biome/policy cases.

Must not:

- Read season profile climate data in this slice.
- Read biome profile climate/environment values in this slice.
- Own YAML parsing.
- Contain Bukkit/Paper logic.

## Out of Scope

Explicitly forbidden work:

- Adding `SeasonProfileProvider` to `CropYieldService`.
- Adding `BiomeProfileProvider` to `CropYieldService`.
- Adding `CropYieldEnvironmentalFactorCalculator`.
- Adding `biomeFactor`.
- Adding `climateYieldFactor`.
- Changing `crop-yields.yml` shape.
- Rebalancing crop multipliers.
- Changing `PaperCropHarvestListener`.
- Changing runtime listener registration.
- Updating gameplay balancing.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- `CropYieldService` or current crop yield test packages do not exist;
- characterization requires broad production refactoring;
- tests cannot be written without introducing Bukkit/Paper/YAML/file I/O into domain tests;
- implementing the test would require changing runtime behavior;
- current code already consumes `SeasonProfileProvider` or `BiomeProfileProvider` for crop yield.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

If useful, also run:

```bash
cd java && ./gradlew test --tests "*CropYieldServiceTest" --no-daemon
```

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Whether crop yield still ignores season profiles and biome profile values.
5. Deferred work.
