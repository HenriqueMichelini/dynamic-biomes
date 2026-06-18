---

id: CROP-YIELD-OPTION-B-RUNTIME-COMPOSITION
type: runtime-composition
bounded_context: pluginruntime
capability: lifecycle
layer: pluginruntime
status: Ready
expected_commit: "feat(crops): wire option b crop yield runtime"
----------------------------------------------------------------

# Goal Card: Wire Option B Crop Yield Runtime Composition

## Status

Ready

## Goal

Update plugin runtime composition so the active crop yield listener uses the full Option B CropYieldService constructor with SeasonProfileProvider and crop yield environmental calculators.

## Why Now

CropYieldService now supports the full Option B calculation path:

```
selectedBaseCropMultiplier
  * cropSeasonalFactor
  * biomeYieldFactor
  * climateYieldFactor
```

However, runtime composition still uses the compatibility constructor, so active crop harvest behavior does not yet use the environmental factors.

All prerequisites are complete:

* crop-yields.yml migrated to crop-specific base multiplier ranges;
* CropYieldService applies Option B when constructed with the full dependency set;
* SeasonProfileProvider exists as a published upstream provider;
* environmental factor calculators exist in crops/yield/domain;
* service-level tests pass;
* YAML resources do not need to change in this slice.

This card is composition-only. It should not change crop yield domain, application calculation behavior, provider parsing, listener drop mutation, or YAML resources.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/012-crop-yield-base-range-yaml-migration.goal.md
* docs/013-crop-yield-option-b-runtime-service.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/PaperCropHarvestListener.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonProfileProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/infrastructure/YamlSeasonProfileProvider.java
* java/app/src/main/resources/season-profiles.yml
* java/app/src/main/resources/crop-yields.yml
* existing pluginruntime/lifecycle tests if present

## Current State

* CropYieldService has a full Option B constructor.
* CropYieldService also has a non-environmental compatibility constructor.
* DynamicBiomes still wires the compatibility path.
* crop-yields.yml already uses crop-specific base multiplier ranges.
* YamlCropYieldPolicyProvider parses the new base-range shape.
* Runtime crop harvest behavior is not yet using biomeYieldFactor or climateYieldFactor.
* PaperCropHarvestListener delegates to CropYieldService and should not need behavior changes.

## Target Runtime Composition

DynamicBiomes should construct the active CropYieldService with:

* BiomeResolver
* CropYieldPolicyProvider
* CurrentSeasonQuery
* SeasonProfileProvider
* CropYieldBiomeFactorCalculator
* CropYieldClimateFactorCalculator
* CropYieldEnvironmentalFactorCalculator
* CropYieldEffectiveMultiplierCalculator
* existing multiplier and quantity calculators or variation sources required by the current constructor

Use exact constructor parameters from the current CropYieldService implementation.

If YamlSeasonProfileProvider is already constructed elsewhere, reuse the existing instance. If not, instantiate it consistently with existing YAML provider construction patterns.

## Runtime Formula After This Card

Active crop harvest runtime should use:

```
adjustedQuantity =
  probabilisticRound(
    vanillaProduceQuantity
      * selectedBaseCropMultiplier
      * cropSeasonalFactor
      * biomeYieldFactor
      * climateYieldFactor
  )
```

where:

```
biomeYieldFactor =
  1.0 + ((fertility - 0.5) * 0.40)
```

and:

```
climateYieldFactor =
  1.0 + (((temperatureAdjustment + humidityAdjustment) / 2.0) * 0.15)
```

## Architectural Boundary

* Bounded context: pluginruntime
* Capability: lifecycle
* Layer: pluginruntime

Rules:

* pluginruntime may import contexts for startup and composition only.
* No non-pluginruntime module may import pluginruntime.
* Do not move crop yield calculation logic into DynamicBiomes.
* Do not parse crop yield, biome profile, or season profile YAML manually in DynamicBiomes.
* Do not change CropYieldService formula in this card.
* Do not change domain calculators in this card.
* Do not change crop-yields.yml.
* Do not change biome-profiles.yml.
* Do not change season-profiles.yml.
* Do not change PaperCropHarvestListener behavior unless constructor signatures force a trivial compile-only update.

## Acceptance Behavior

1. DynamicBiomes wires CropYieldService using the full Option B constructor.

2. Runtime crop harvest yield uses environmental factors through CropYieldService, not through listener logic.

3. YamlSeasonProfileProvider is constructed or reused through the same resource/data-folder pattern as other YAML profile providers.

4. PaperCropHarvestListener remains behaviorally unchanged.

5. crop-yields.yml, biome-profiles.yml, and season-profiles.yml are unchanged.

6. CropYieldService tests still pass.

7. Full build passes.

8. ARCHITECTURE.md is updated to move Option B runtime crop yield wiring from deferred/transitional status to active runtime behavior.

## TDD Plan

1. Inspect existing pluginruntime composition tests, if any.
2. If there is a practical composition test seam, add or update a test first to assert DynamicBiomes uses the full CropYieldService wiring.
3. If no safe pluginruntime test seam exists, document that runtime composition is verified by compile/build and targeted diff inspection.
4. Update DynamicBiomes with the smallest composition change.
5. Run focused crop yield service tests to ensure application behavior still passes.
6. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java

Tests:

* Existing pluginruntime/lifecycle test only if a safe seam exists.

Documentation:

* ARCHITECTURE.md

Not expected:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/PaperCropHarvestListener.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/resources/biome-profiles.yml
* java/app/src/main/resources/season-profiles.yml

## Responsibility / Collaboration Notes

Class: DynamicBiomes

Responsibility:

* Create runtime provider and service instances.
* Wire listener dependencies.
* Register listeners.
* Keep composition logic thin.

Must not:

* Calculate crop yield multipliers.
* Derive biome or climate factors directly.
* Parse YAML manually.
* Contain crop yield business rules.

Class: PaperCropHarvestListener

Responsibility:

* Translate Bukkit crop harvest events into CropYieldService calls.
* Replace produce drops only when adjusted quantity differs.

Must not:

* Derive environmental factors.
* Query season profiles.
* Query biome profiles directly.
* Parse config.

## Out of Scope

Explicitly forbidden work:

* Changing crop yield domain formulas.
* Changing CropYieldService behavior.
* Changing YamlCropYieldPolicyProvider.
* Changing crop-yields.yml.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Cleaning up the CropYieldService compatibility constructor.
* Cleaning up CropYieldPolicyProvider.policyFor(BiomeId).
* Adding reload commands.
* Adding diagnostics or presentation output.
* Rebalancing crop yield values.
* Supporting legacy crop-yields.yml shape.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md conflicts with this card.
* AGENTS.md conflicts with this card.
* YamlSeasonProfileProvider cannot be constructed without broader configuration lifecycle changes.
* DynamicBiomes lacks access to the data folder/resource-copy pattern required by YamlSeasonProfileProvider.
* wiring the full CropYieldService constructor requires changing application/domain behavior.
* implementing this requires changing YAML resources.
* implementing this requires broad pluginruntime refactoring.
* tests would require unsafe Bukkit/Paper event simulation.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*CropYieldServiceTest*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Also inspect:

```
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
git diff -- java/app/src/main/resources
git grep -n "new CropYieldService" -- java/app/src/main/java
git grep -n "YamlSeasonProfileProvider" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Whether a runtime composition test was added or why it was deferred.
3. Exact constructor/wiring path used.
4. Commands run and results.
5. Confirmation that YAML resources did not change.
6. Confirmation that crop yield environmental factors are now active at runtime.
7. Deferred cleanup work.
