---

id: CROP-YIELD-OPTION-B-MULTIPLIER-DOMAIN
type: domain
bounded_context: crops
capability: yield
layer: domain
status: Ready
expected_commit: "feat(crops): compose option b crop yield multiplier"
----------------------------------------------------------------------

# Goal Card: Compose Option B Crop Yield Multiplier

## Status

Ready

## Goal

Add pure crop-yield domain behavior that composes the full Option B effective multiplier from a selected base crop multiplier, cropSeasonalFactor, and CropYieldEnvironmentalFactor, without changing runtime crop harvest behavior.

## Why Now

Option B is now documented and partially modeled.

Already implemented:

* CropYieldEnvironmentalFactor
* CropYieldEnvironmentalFactorCalculator
* CropYieldBiomeFactorCalculator
* CropYieldClimateFactorCalculator

The remaining pure domain gap is the final Option B multiplier composition:

```
effectiveMultiplier =
  selectedBaseCropMultiplier
  * cropSeasonalFactor
  * environmentalFactor
```

This card implements only the pure domain composition step. Runtime wiring and crop-yields.yml migration remain deferred.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* docs/005-crop-yield-environmental-factor-domain.goal.md
* docs/008-crop-yield-factor-conversion-formulas.goal.md
* docs/009-crop-yield-factor-derivation-domain.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldMultiplierCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldMultiplierRange.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldSeasonalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactor.java
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/

## Current State

* Current runtime crop yield still uses the existing crop yield multiplier path.
* crop-yields.yml still contains biome-scoped multiplier ranges.
* CropYieldService still does not consume BiomeProfileProvider or SeasonProfileProvider.
* CropYieldService still does not use the new environmental factor derivation calculators.
* Runtime crop harvest behavior must remain unchanged in this card.

## Target Domain Semantics

Add a pure domain calculator for the full Option B multiplier composition.

Formula:

```
effectiveMultiplier =
  selectedBaseCropMultiplier
  * cropSeasonalFactor
  * environmentalFactor
```

Definitions:

```
selectedBaseCropMultiplier:
  already-selected multiplier from a future base crop range

cropSeasonalFactor:
  crop-yield-owned seasonal factor from crop-yields.yml

environmentalFactor:
  biomeYieldFactor * climateYieldFactor
```

The calculator should not select random values from a range. It should compose already-selected or already-derived values only.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: domain

Rules:

* Domain code must remain pure Java.
* Domain code must not import Bukkit, Paper, YAML, file I/O, pluginruntime, or infrastructure.
* Do not modify CropYieldService.
* Do not modify PaperCropHarvestListener.
* Do not modify DynamicBiomes runtime wiring.
* Do not modify crop-yields.yml.
* Do not modify biome-profiles.yml.
* Do not modify season-profiles.yml.
* Do not migrate existing biome-scoped multiplier ranges.
* Do not apply Option B to runtime behavior in this card.

## Recommended Design

Prefer a small explicit calculator, for example:

* CropYieldEffectiveMultiplierCalculator

The calculator should accept:

* selected base multiplier
* CropYieldSeasonalFactor
* CropYieldEnvironmentalFactor

Return either:

* a double, if existing crop yield multiplier calculators return primitive doubles;
* or an existing/new value object only if that matches current domain conventions.

Do not introduce generic abstractions such as:

* EffectType
* BiomeEffect
* EnvironmentalEffect
* GenericMultiplier
* MultiplierPipeline

Do not modify the existing CropYieldMultiplierCalculator unless the existing design clearly makes that the smallest safe option. If modifying it would affect current runtime behavior, stop and report instead.

## Acceptance Behavior

1. Given selectedBaseCropMultiplier 1.00, cropSeasonalFactor 1.00, and environmentalFactor 1.00, when composing the Option B multiplier, then the result is 1.00.

2. Given selectedBaseCropMultiplier 1.25, cropSeasonalFactor 1.10, and environmentalFactor 0.96, when composing the Option B multiplier, then the result is 1.32.

3. Given selectedBaseCropMultiplier 0.80, cropSeasonalFactor 1.00, and environmentalFactor 1.20, when composing the Option B multiplier, then the result is 0.96.

4. Given selectedBaseCropMultiplier 1.00, cropSeasonalFactor 0.00, and environmentalFactor 1.20, when composing the Option B multiplier, then the result is 0.00 if existing CropYieldSeasonalFactor permits zero.

5. Given invalid selectedBaseCropMultiplier values such as negative, NaN, or infinite, then the calculator rejects them with an exception consistent with current domain conventions.

6. Given null cropSeasonalFactor or null environmentalFactor, then the calculator rejects them with an exception consistent with current domain conventions.

7. The new behavior does not select random multipliers, read providers, resolve biome, query current season, parse YAML, access files, import Bukkit/Paper, or mutate runtime state.

## TDD Plan

1. Inspect existing crop yield domain numeric validation conventions.
2. Add focused domain tests first.
3. Confirm tests fail for missing production behavior.
4. Implement the smallest pure domain calculator.
5. Run focused tests.
6. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEffectiveMultiplierCalculator.java

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEffectiveMultiplierCalculatorTest.java

Documentation:

* ARCHITECTURE.md only if the target structure or implemented-support notes must list the new calculator.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: CropYieldEffectiveMultiplierCalculator

Responsibility:

* Compose the full Option B multiplier from already-selected and already-derived factors.
* Keep the formula deterministic and testable.
* Validate invalid numeric input according to existing domain conventions.

Must not:

* Select a multiplier from CropYieldMultiplierRange.
* Read CropYieldPolicyProvider.
* Read BiomeProfileProvider.
* Read SeasonProfileProvider.
* Resolve biome.
* Query current season.
* Read YAML.
* Affect runtime crop harvest behavior.

## Out of Scope

Explicitly forbidden work:

* Changing CropYieldService.
* Changing CropYieldMultiplierCalculator if doing so affects current runtime behavior.
* Changing CropYieldQuantityCalculator.
* Changing CropYieldPolicy.
* Changing CropYieldPolicyProvider.
* Adding SeasonProfileProvider to Crop yield application services.
* Adding BiomeProfileProvider to crop yield application services.
* Changing crop-yields.yml.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Changing PaperCropHarvestListener.
* Changing DynamicBiomes runtime wiring.
* Rebalancing crop yield values.
* Migrating crop-yields.yml from biome-scoped ranges to base crop ranges.
* Adding application or infrastructure classes.
* Adding generic effect abstractions.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md conflicts with this card.
* AGENTS.md conflicts with this card.
* existing crop yield domain conventions require modifying the current runtime multiplier calculator.
* implementing this would change current runtime crop harvest behavior.
* implementing this requires provider injection or service orchestration.
* implementing this requires YAML/resource changes.
* tests would require Bukkit, Paper, YAML, file I/O, or database access.
* this task would create unused application or infrastructure scaffolding.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*CropYieldEffectiveMultiplierCalculator*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Exact formula implemented.
4. Commands run and results.
5. Confirmation that runtime crop harvest behavior did not change.
6. Confirmation that YAML resources were not changed.
7. Deferred work.
