---

id: CROP-YIELD-ENVIRONMENTAL-FACTOR-DOMAIN
type: domain
bounded_context: crops
capability: yield
layer: domain
status: Ready
expected_commit: "feat(crops): model crop yield environmental factors"
----------------------------------------------------------------------

# Goal Card: Model Crop Yield Environmental Factors

## Status

Ready

## Goal

Add a pure domain model and calculator for crop yield environmental factors that can later support Option B without changing runtime crop harvest behavior in this slice.

## Why Now

Option B has been selected and documented as the future crop yield design:

```
effectiveMultiplier =
  selectedBaseCropMultiplier
  * biomeYieldFactor
  * cropSeasonalFactor
  * climateYieldFactor
```

The current runtime still uses biome-scoped crop yield multiplier ranges from crop-yields.yml. Before wiring biome and season profiles into CropYieldService, the environmental factor calculation should be modeled and tested in isolation.

This slice creates the pure crop-yield domain behavior only. It must not change runtime behavior, YAML shape, listener behavior, or service orchestration.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldMultiplierCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldMultiplierRange.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldSeasonalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/profile/domain/
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/

## Current State

* Crop yield currently calculates final produce quantity from a selected crop yield multiplier and a crop-owned seasonal factor.
* The selected multiplier is currently biome-scoped through crop-yields.yml.
* Characterization tests document that crop yield ignores season profiles and biome profile climate/environment values.
* ARCHITECTURE.md now records Option B as the selected future model.
* Runtime behavior must remain unchanged in this card.

## Target Domain Semantics

Introduce a crop-yield-owned environmental factor calculation with these conceptual factors:

```
biomeYieldFactor:
  environmental contribution derived from already-resolved biome profile values or explicit crop-yield-owned biome factor data

climateYieldFactor:
  environmental contribution derived from already-resolved season climate adjustment values

environmentalFactor:
  biomeYieldFactor * climateYieldFactor
```

The domain calculator should not resolve biome, query current season, read YAML, or know about Bukkit.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: domain

Rules:

* Domain code must remain pure Java.
* Domain code must not import Bukkit, Paper, YAML, file I/O, pluginruntime, or upstream infrastructure.
* Domain code may import only published upstream domain vocabulary if needed.
* Do not modify CropYieldService in this card.
* Do not modify PaperCropHarvestListener in this card.
* Do not modify DynamicBiomes runtime wiring in this card.
* Do not modify crop-yields.yml in this card.
* Do not migrate existing biome-scoped multiplier ranges in this card.

## Recommended Design

Prefer a small, explicit model over generic effect abstractions.

Expected production candidates:

* CropYieldEnvironmentalFactor
* CropYieldEnvironmentalFactorCalculator

A minimal acceptable model is:

```
CropYieldEnvironmentalFactor:
  validated value object around a non-negative double factor

CropYieldEnvironmentalFactorCalculator:
  combines already-derived biomeYieldFactor and climateYieldFactor into one environmental factor
```

If the existing value object conventions favor records, use records with constructor validation.

Avoid names such as:

* BiomeEffect
* EffectType
* CropEffect
* GenericMultiplier
* EnvironmentalEffect

Those names are too broad and conflict with the architecture’s anti-coupling guidance.

## Acceptance Behavior

1. Given biome yield factor 1.20 and climate yield factor 0.80, when the calculator combines them, then the environmental factor is 0.96.

2. Given biome yield factor 1.00 and climate yield factor 1.00, when the calculator combines them, then the environmental factor is neutral 1.00.

3. Given biome yield factor 0.00 and climate yield factor 1.00, when the calculator combines them, then the environmental factor is 0.00.

4. Given any negative factor input, then construction or calculation rejects it with an appropriate exception.

5. Given very high positive factors, then the calculator preserves the mathematical product unless an existing project convention requires an explicit cap.

6. The new domain model has no side effects and does not consume providers, resolvers, Bukkit objects, YAML, files, or runtime services.

## TDD Plan

1. Add tests first under the crop yield domain test package.
2. Cover neutral, multiplicative, zero, and negative-input cases.
3. Confirm the tests fail for missing production classes.
4. Implement the smallest domain classes needed.
5. Run focused domain tests.
6. Run the full verification command.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculator.java

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorTest.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculatorTest.java

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: CropYieldEnvironmentalFactor

Responsibility:

* Represent one validated crop yield environmental multiplier.
* Accept zero and positive values.
* Reject negative, NaN, or infinite values if that matches existing numeric validation conventions.

Must not:

* Know whether the factor came from biome, season, YAML, or runtime services.
* Resolve biome.
* Query season.
* Read configuration.

Class: CropYieldEnvironmentalFactorCalculator

Responsibility:

* Combine already-derived environmental factors.
* Keep calculation deterministic and testable.

Must not:

* Depend on CropYieldService.
* Depend on provider ports.
* Depend on Bukkit/Paper.
* Depend on YAML or files.
* Change crop harvest behavior.

## Out of Scope

Explicitly forbidden work:

* Changing CropYieldService.
* Changing CropYieldMultiplierCalculator.
* Changing CropYieldQuantityCalculator.
* Adding SeasonProfileProvider to crop yield service.
* Adding BiomeProfileProvider to crop yield service.
* Changing crop-yields.yml.
* Changing season-profiles.yml.
* Changing biome-profiles.yml.
* Changing runtime wiring.
* Changing PaperCropHarvestListener.
* Rebalancing crop yield values.
* Migrating biome-scoped ranges to base crop ranges.
* Adding generic effect abstractions.
* Adding unused application or infrastructure classes.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md conflicts with this card.
* AGENTS.md conflicts with this card.
* existing domain numeric validation conventions require a different value object shape.
* implementing this requires changing runtime crop yield behavior.
* implementing this requires provider injection or service orchestration.
* tests would require Bukkit, Paper, YAML, file I/O, or database access.
* the task would create unused application or infrastructure scaffolding.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*CropYieldEnvironmentalFactor*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Confirmation that runtime crop harvest behavior did not change.
5. Confirmation that crop-yields.yml was not changed.
6. Deferred work.
