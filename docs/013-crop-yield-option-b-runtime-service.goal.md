---

id: CROP-YIELD-OPTION-B-RUNTIME-SERVICE
type: application
bounded_context: crops
capability: yield
layer: application
status: Ready
expected_commit: "feat(crops): apply option b crop yield factors"
-----------------------------------------------------------------

# Goal Card: Apply Option B Crop Yield Factors in CropYieldService

## Status

Ready

## Goal

Wire Option B crop yield calculation into CropYieldService by applying derived biome and climate environmental factors to the migrated crop-specific base multiplier ranges.

## Why Now

The required prerequisites are complete:

* current behavior was characterized;
* Option B was selected and documented;
* exact factor conversion formulas were documented;
* environmental factor domain model exists;
* biome and climate factor derivation calculators exist;
* effective multiplier composition exists;
* crop-yields.yml has migrated from biome-scoped ranges to crop-specific base ranges.

Runtime can now safely apply biomeYieldFactor and climateYieldFactor without double-counting biome influence.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/012-crop-yield-base-range-yaml-migration.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldBiomeFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldClimateFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEffectiveMultiplierCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/resolution/domain/BiomeResolver.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/resolution/domain/BiomeContext.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonProfileProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/cycle/domain/CurrentSeasonQuery.java
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/

## Current State

* crop-yields.yml now uses crop-specific base multiplier ranges.
* YamlCropYieldPolicyProvider parses the new base-range shape.
* CropYieldPolicyProvider.policyFor(BiomeId) still exists and the provider ignores BiomeId as transitional behavior.
* CropYieldService currently applies selectedBaseCropMultiplier and cropSeasonalFactor only.
* CropYieldService does not yet apply biomeYieldFactor or climateYieldFactor.
* CropYieldService does not yet consume SeasonProfileProvider.
* BiomeResolver may already return BiomeContext containing BiomeProfile.

## Target Runtime Formula

CropYieldService should calculate:

```
adjustedQuantity =
  probabilisticRound(
    vanillaProduceQuantity
    * effectiveMultiplier
  )
```

where:

```
biomeYieldFactor =
  CropYieldBiomeFactorCalculator.derive(biomeContext.profile().climateProfile().fertility())

climateYieldFactor =
  CropYieldClimateFactorCalculator.derive(currentSeasonProfile.climateAdjustment())

environmentalFactor =
  CropYieldEnvironmentalFactorCalculator.combine(biomeYieldFactor, climateYieldFactor)

effectiveMultiplier =
  CropYieldEffectiveMultiplierCalculator.compose(
    selectedBaseCropMultiplier,
    cropSeasonalFactor,
    environmentalFactor
  )
```

Use exact accessor names from the current domain model.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: application

Rules:

* CropYieldService may consume published upstream domain ports and vocabulary.
* CropYieldService must not import biome or seasons infrastructure.
* CropYieldService must not import Bukkit, Paper, YAML, file I/O, or pluginruntime.
* Do not modify PaperCropHarvestListener.
* Do not modify crop-yields.yml.
* Do not modify biome-profiles.yml.
* Do not modify season-profiles.yml.
* Do not change YAML provider parsing in this card unless a compile-time contract issue requires a very small adaptation.
* Do not add generic effect abstractions.

## Implementation Guidance

Prefer using the already-resolved BiomeContext from BiomeResolver for biome profile data if it exposes BiomeProfile.

Add SeasonProfileProvider to CropYieldService only if needed to get the current SeasonProfile for the current SeasonId.

Do not add BiomeProfileProvider if BiomeResolver already returns BiomeContext with BiomeProfile. Only add BiomeProfileProvider if the current BiomeContext does not expose the needed profile data.

Preserve unsupported fallback semantics:

* UnsupportedBiomeException should still preserve vanilla quantity.
* UnsupportedCropYieldPolicyException should still preserve vanilla quantity.

If SeasonProfileProvider can throw an explicit unsupported profile exception, decide based on existing architecture docs and tests. Prefer preserving vanilla quantity only for explicit unsupported configuration cases already documented; do not swallow malformed YAML, invalid values, or programming errors.

## Acceptance Behavior

1. Given a biome profile with fertility 0.50 and season climate temperature/humidity adjustment 0.00/0.00, when CropYieldService calculates produce quantity, then environmental influence is neutral.

2. Given fertility 1.00 and neutral climate adjustment, when CropYieldService calculates produce quantity, then biomeYieldFactor 1.20 is applied.

3. Given fertility 0.00 and neutral climate adjustment, when CropYieldService calculates produce quantity, then biomeYieldFactor 0.80 is applied.

4. Given neutral fertility and maximum positive temperature/humidity climate adjustment, when CropYieldService calculates produce quantity, then climateYieldFactor 1.15 is applied.

5. Given neutral fertility and maximum negative temperature/humidity climate adjustment, when CropYieldService calculates produce quantity, then climateYieldFactor 0.85 is applied.

6. Given selectedBaseCropMultiplier 1.25, cropSeasonalFactor 1.10, fertility-derived biomeYieldFactor 1.20, and climateYieldFactor 1.00, when CropYieldService calculates produce quantity, then the effective multiplier is 1.65.

7. Unsupported biome or unsupported crop yield policy still preserves vanilla produce quantity.

8. CropYieldService imports only published upstream domain contracts, not upstream infrastructure.

9. crop-yields.yml remains unchanged in this card.

10. PaperCropHarvestListener remains unchanged in this card unless constructor compilation requires a later runtime-composition card instead.

## TDD Plan

1. Update CropYieldService application tests first with in-memory fakes/stubs for:

   * BiomeResolver
   * CropYieldPolicyProvider
   * CurrentSeasonQuery
   * SeasonProfileProvider, if needed
2. Add neutral environmental factor test.
3. Add biome fertility factor tests.
4. Add season climate factor tests.
5. Add combined effective multiplier test.
6. Confirm tests fail against current service behavior.
7. Implement the smallest CropYieldService change.
8. Run focused service tests.
9. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldServiceTest.java

Documentation:

* ARCHITECTURE.md if implemented runtime behavior or constructor dependencies change.

Not expected in this card:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/PaperCropHarvestListener.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/resources/biome-profiles.yml
* java/app/src/main/resources/season-profiles.yml

## Responsibility / Collaboration Notes

Class: CropYieldService

Responsibility:

* Resolve biome context.
* Load crop yield base policy.
* Read current season.
* Load current season profile if required.
* Derive biomeYieldFactor.
* Derive climateYieldFactor.
* Combine environmental factors.
* Compose Option B effective multiplier.
* Delegate final quantity rounding to existing quantity calculator.
* Preserve vanilla quantity for explicit unsupported fallback cases.

Must not:

* Parse YAML.
* Read files.
* Handle Bukkit events.
* Mutate block drops.
* Import upstream infrastructure.
* Own biome or season profile data.

## Out of Scope

Explicitly forbidden work:

* Changing crop-yields.yml.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Changing YamlCropYieldPolicyProvider.
* Changing PaperCropHarvestListener.
* Changing DynamicBiomes runtime wiring.
* Cleaning up CropYieldPolicyProvider.policyFor(BiomeId).
* Supporting legacy crop-yields.yml shape.
* Rebalancing values.
* Adding new crop types.
* Adding generic effect abstractions.
* Adding presentation diagnostics.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md conflicts with this card.
* AGENTS.md conflicts with this card.
* BiomeContext does not expose enough published biome profile data and adding BiomeProfileProvider would create broader wiring changes.
* SeasonProfileProvider does not expose enough published season profile data.
* implementing this requires upstream infrastructure imports.
* implementing this requires changing YAML/resource files.
* implementing this requires listener or pluginruntime changes in the same card.
* tests would require Bukkit/Paper event testing.
* unsupported season profile fallback semantics are unclear and cannot be inferred from existing architecture/tests.

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
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldServiceTest.java
git diff -- java/app/src/main/resources
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Exact runtime formula after this card.
4. Whether SeasonProfileProvider was added to CropYieldService.
5. Whether BiomeProfileProvider was needed.
6. Commands run and results.
7. Confirmation that YAML resources were not changed.
8. Deferred runtime-composition work.
