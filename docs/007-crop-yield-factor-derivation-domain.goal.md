---

id: CROP-YIELD-FACTOR-DERIVATION-DOMAIN
type: domain
bounded_context: crops
capability: yield
layer: domain
status: Ready
expected_commit: "feat(crops): derive crop yield environmental factors"
-----------------------------------------------------------------------

# Goal Card: Derive Crop Yield Environmental Factors

## Status

Ready

## Goal

Add pure crop-yield domain calculators that derive biomeYieldFactor and climateYieldFactor from already-provided published biome and season profile values, without wiring them into runtime crop harvest behavior.

## Why Now

Option B is now documented as the selected future model:

```
effectiveMultiplier =
  selectedBaseCropMultiplier
  * biomeYieldFactor
  * cropSeasonalFactor
  * climateYieldFactor
```

The environmental factor value object and combiner already exist. The next missing domain slice is derivation of the two environmental factor inputs:

```
biomeYieldFactor
climateYieldFactor
```

This card must keep derivation pure and isolated. Runtime service wiring, YAML migration, and balancing changes remain deferred.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* docs/005-crop-yield-environmental-factor-domain.goal.md
* docs/006-crop-yield-factor-derivation-semantics.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/profile/domain/
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/

## Current State

* CropYieldEnvironmentalFactor exists as a finite non-negative value object.
* CropYieldEnvironmentalFactorCalculator multiplies biome and climate factors.
* ARCHITECTURE.md documents that first-version biomeYieldFactor should derive primarily from Fertility.
* ARCHITECTURE.md documents that first-version climateYieldFactor should derive from season climate temperature and humidity adjustment.
* CropYieldService is still not wired to BiomeProfileProvider or SeasonProfileProvider.
* crop-yields.yml still uses legacy/current biome-scoped multiplier ranges.
* Runtime crop harvest behavior must remain unchanged in this card.

## Target Domain Semantics

Add crop-yield-owned derivation calculators.

### biomeYieldFactor

The first implementation should derive biomeYieldFactor primarily from published biome fertility data.

Preferred neutral mapping:

```
fertility 1.00 -> factor 1.00
```

Preferred direct mapping if the current Fertility value object already represents a normalized multiplier-like value:

```
biomeYieldFactor = fertility.value
```

If Fertility uses another scale, such as 0..100, follow the existing project convention. If no convention is clear, stop and report instead of inventing an arbitrary conversion.

### climateYieldFactor

The first implementation should derive climateYieldFactor from published season climate adjustment data.

Use temperature and humidity adjustment only.

Preferred conservative mapping if the existing SeasonalAdjustment values already represent multiplier-like values:

```
climateYieldFactor =
  average(temperatureAdjustment.value, humidityAdjustment.value)
```

If the values are deltas rather than multiplier-like factors, use the existing project convention if one exists. If no convention is clear, stop and report instead of inventing an arbitrary conversion.

### Double-Counting Constraint

Do not apply these derived factors to runtime crop yield yet.

This card only derives factors in pure domain code. The existing biome-scoped crop-yields.yml ranges must not be multiplied by the new biomeYieldFactor.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: domain

Rules:

* Domain code must remain pure Java.
* Domain code must not import Bukkit, Paper, YAML, file I/O, pluginruntime, or upstream infrastructure.
* Domain code may import published upstream domain vocabulary only if needed.
* Do not modify CropYieldService.
* Do not modify PaperCropHarvestListener.
* Do not modify DynamicBiomes runtime wiring.
* Do not modify crop-yields.yml.
* Do not modify biome-profiles.yml.
* Do not modify season-profiles.yml.
* Do not migrate existing biome-scoped multiplier ranges.

## Recommended Design

Prefer explicit crop-yield-owned calculators:

* CropYieldBiomeFactorCalculator
* CropYieldClimateFactorCalculator

Accept constructor/static-method style according to existing domain conventions.

Do not introduce generic abstractions such as:

* BiomeEffect
* EffectType
* EnvironmentalEffect
* GenericFactorCalculator
* CropEffect

The calculators should be crop-yield-owned because crop yield owns the interpretation of environmental values into yield behavior.

## Acceptance Behavior

1. Given neutral fertility, when deriving biomeYieldFactor, then the result is neutral 1.00.

2. Given fertility above neutral, when deriving biomeYieldFactor, then the result is above 1.00 according to the documented mapping.

3. Given fertility below neutral, when deriving biomeYieldFactor, then the result is below 1.00 according to the documented mapping, unless the existing Fertility model disallows below-neutral values.

4. Given neutral temperature and humidity season climate adjustments, when deriving climateYieldFactor, then the result is neutral 1.00.

5. Given one above-neutral and one below-neutral season climate adjustment, when deriving climateYieldFactor, then the result reflects the documented combination rule.

6. Given null inputs, then the calculators reject them with an appropriate exception consistent with current domain conventions.

7. The new calculators do not read providers, resolve biome, query current season, parse YAML, access files, import Bukkit/Paper, or mutate runtime state.

## TDD Plan

1. Inspect existing Fertility, SeasonClimateAdjustment, and SeasonalAdjustment APIs.
2. Add focused domain tests first.
3. Confirm tests fail for missing production calculators.
4. Implement the smallest pure domain calculators.
5. Run focused tests.
6. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldBiomeFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldClimateFactorCalculator.java

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldBiomeFactorCalculatorTest.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldClimateFactorCalculatorTest.java

Documentation:

* ARCHITECTURE.md only if the implemented mapping differs from the already documented first-version semantics or if exact formulas need to be recorded.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: CropYieldBiomeFactorCalculator

Responsibility:

* Convert already-provided published biome fertility data into a CropYieldEnvironmentalFactor.
* Keep the formula deterministic and testable.
* Avoid resolving biome or reading profile providers.

Must not:

* Import biome infrastructure.
* Know about Bukkit biomes.
* Read biome-profiles.yml.
* Apply the factor to crop harvest runtime behavior.

Class: CropYieldClimateFactorCalculator

Responsibility:

* Convert already-provided season climate adjustment data into a CropYieldEnvironmentalFactor.
* Use temperature and humidity adjustment according to documented semantics.
* Keep the formula deterministic and testable.

Must not:

* Import seasons infrastructure.
* Query current season.
* Read season-profiles.yml.
* Apply the factor to crop harvest runtime behavior.

## Out of Scope

Explicitly forbidden work:

* Changing CropYieldService.
* Changing CropYieldMultiplierCalculator.
* Changing CropYieldQuantityCalculator.
* Changing CropYieldPolicy.
* Changing CropYieldPolicyProvider.
* Adding SeasonProfileProvider to CropYieldService.
* Adding BiomeProfileProvider to CropYieldService.
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
* Fertility does not expose enough information to derive a factor.
* SeasonClimateAdjustment or SeasonalAdjustment does not expose enough information to derive a factor.
* existing numeric semantics are unclear and no documented conversion exists.
* implementation requires changing runtime crop yield behavior.
* implementation requires provider injection or service orchestration.
* tests would require Bukkit, Paper, YAML, file I/O, or database access.
* this task would require changing YAML shape or balancing values.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*CropYield*FactorCalculator*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Exact derivation formulas implemented.
4. Commands run and results.
5. Confirmation that runtime crop harvest behavior did not change.
6. Confirmation that YAML resources were not changed.
7. Deferred work.
