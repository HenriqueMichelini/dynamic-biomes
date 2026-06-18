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

Option B is now documented as the selected future crop yield model:

```
effectiveMultiplier =
  selectedBaseCropMultiplier
  * biomeYieldFactor
  * cropSeasonalFactor
  * climateYieldFactor
```

The exact conversion formulas are now documented in ARCHITECTURE.md, so the previous blocker is resolved.

This card implements only the pure domain derivation calculators. Runtime service wiring, YAML migration, and balancing changes remain deferred.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* docs/005-crop-yield-environmental-factor-domain.goal.md
* docs/006-crop-yield-factor-derivation-semantics.goal.md
* docs/008-crop-yield-factor-conversion-formulas.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/profile/domain/Fertility.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonalAdjustment.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonClimateAdjustment.java
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/

## Current State

* CropYieldEnvironmentalFactor exists as a finite non-negative value object.
* CropYieldEnvironmentalFactorCalculator multiplies biome and climate factors.
* ARCHITECTURE.md documents the exact biomeYieldFactor and climateYieldFactor formulas.
* CropYieldService is still not wired to BiomeProfileProvider or SeasonProfileProvider.
* crop-yields.yml still uses legacy/current biome-scoped multiplier ranges.
* Runtime crop harvest behavior must remain unchanged in this card.

## Target Domain Semantics

Add crop-yield-owned derivation calculators.

### biomeYieldFactor

Fertility is normalized in [0.0, 1.0].

Use midpoint-neutral mapping:

```
neutral fertility = 0.5

biomeYieldFactor =
  1.0 + ((fertility - 0.5) * 0.40)
```

Expected examples:

```
fertility 0.00 -> biomeYieldFactor 0.80
fertility 0.25 -> biomeYieldFactor 0.90
fertility 0.50 -> biomeYieldFactor 1.00
fertility 0.75 -> biomeYieldFactor 1.10
fertility 1.00 -> biomeYieldFactor 1.20
```

### climateYieldFactor

SeasonalAdjustment is normalized in [-1.0, 1.0].

Use zero-neutral mapping:

```
neutral adjustment = 0.0
```

Use temperature and humidity only:

```
averageAdjustment =
  (temperatureAdjustment + humidityAdjustment) / 2.0

climateYieldFactor =
  1.0 + (averageAdjustment * 0.15)
```

Expected examples:

```
temperature 0.00, humidity 0.00 -> climateYieldFactor 1.000
temperature 1.00, humidity 1.00 -> climateYieldFactor 1.150
temperature -1.00, humidity -1.00 -> climateYieldFactor 0.850
temperature 1.00, humidity -1.00 -> climateYieldFactor 1.000
temperature 0.50, humidity -0.50 -> climateYieldFactor 1.000
```

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
* Domain code may import published upstream domain vocabulary only.
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

Do not introduce generic abstractions such as:

* BiomeEffect
* EffectType
* EnvironmentalEffect
* GenericFactorCalculator
* CropEffect

The calculators should be crop-yield-owned because crop yield owns the interpretation of environmental values into yield behavior.

## Acceptance Behavior

1. Given fertility 0.50, when deriving biomeYieldFactor, then the result is 1.00.

2. Given fertility 1.00, when deriving biomeYieldFactor, then the result is 1.20.

3. Given fertility 0.00, when deriving biomeYieldFactor, then the result is 0.80.

4. Given fertility 0.75, when deriving biomeYieldFactor, then the result is 1.10.

5. Given fertility 0.25, when deriving biomeYieldFactor, then the result is 0.90.

6. Given neutral temperature and humidity season climate adjustments, when deriving climateYieldFactor, then the result is 1.00.

7. Given maximum positive temperature and humidity adjustments, when deriving climateYieldFactor, then the result is 1.15.

8. Given maximum negative temperature and humidity adjustments, when deriving climateYieldFactor, then the result is 0.85.

9. Given opposite maximum temperature and humidity adjustments, when deriving climateYieldFactor, then the result is 1.00.

10. Given null inputs, then the calculators reject them with an appropriate exception consistent with current domain conventions.

11. The new calculators do not read providers, resolve biome, query current season, parse YAML, access files, import Bukkit/Paper, or mutate runtime state.

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

* ARCHITECTURE.md only if the implemented formulas differ from the documented formulas or if target structure must list the new classes.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: CropYieldBiomeFactorCalculator

Responsibility:

* Convert already-provided published biome fertility data into a CropYieldEnvironmentalFactor.
* Implement the formula exactly as documented.
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
* Implement the formula exactly as documented.
* Use temperature and humidity only.
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
* Fertility is not normalized in [0.0, 1.0].
* SeasonalAdjustment is not normalized in [-1.0, 1.0].
* SeasonClimateAdjustment does not expose temperature and humidity adjustments.
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
