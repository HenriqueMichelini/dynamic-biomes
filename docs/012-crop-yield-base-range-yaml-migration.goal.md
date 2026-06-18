---

id: CROP-YIELD-BASE-RANGE-YAML-MIGRATION
type: infrastructure
bounded_context: crops
capability: yield
layer: infrastructure
status: Ready
expected_commit: "feat(crops): migrate crop yield config to base ranges"
------------------------------------------------------------------------

# Goal Card: Migrate Crop Yield Config to Base Ranges

## Status

Ready

## Goal

Migrate crop-yields.yml and YamlCropYieldPolicyProvider from legacy biome-scoped crop yield ranges to Option B crop-specific base multiplier ranges, without wiring environmental factors into runtime crop harvest behavior.

## Why Now

Option B crop yield semantics are documented.

Already complete:

* crop yield current behavior characterization;
* Option B formula decision;
* environmental factor value object and combiner;
* biome and climate factor derivation calculators;
* effective multiplier composition calculator;
* target crop-yields.yml semantics.

The next blocker before runtime Option B wiring is the active YAML shape. Runtime must not multiply biomeYieldFactor on top of the current biome-scoped multiplier ranges. Therefore crop-yields.yml must first move to crop-specific base multiplier ranges.

This card performs the configuration migration only. It does not wire BiomeProfileProvider, SeasonProfileProvider, or environmental factor calculators into CropYieldService.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/011-crop-yield-option-b-yaml-semantics.goal.md
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicy.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldCropRule.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* existing tests under java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/

## Current State

* crop-yields.yml currently stores crop yield multiplier ranges under biomes.
* YamlCropYieldPolicyProvider currently parses legacy biome-scoped policies.
* CropYieldService currently resolves BiomeId and loads a crop yield policy through CropYieldPolicyProvider.
* CropYieldService applies selected multiplier and crop-owned seasonal factor only.
* CropYieldService does not consume BiomeProfileProvider or SeasonProfileProvider.
* Option B environmental calculators exist but are not wired.

## Target YAML Semantics

Migrate crop-yields.yml to crop-specific base multiplier ranges.

Preferred shape:

```
crops:
  wheat:
    base-multiplier:
      min: 0.90
      max: 1.10
    seasonal-factors:
      spring: 1.05
      summer: 1.00
      autumn: 1.00
      winter: 0.90

  carrots:
    base-multiplier:
      min: 0.90
      max: 1.10
    seasonal-factors:
      spring: 1.00
      summer: 1.05
      autumn: 1.00
      winter: 0.90

  potatoes:
    base-multiplier:
      min: 0.90
      max: 1.10
    seasonal-factors:
      spring: 1.00
      summer: 1.00
      autumn: 1.05
      winter: 0.90

  beetroot:
    base-multiplier:
      min: 0.90
      max: 1.10
    seasonal-factors:
      spring: 1.00
      summer: 1.00
      autumn: 1.05
      winter: 0.90
```

Use the existing crop and season IDs already supported by the repository. If the current resource uses materially different balancing, choose conservative base values and document the change in the report.

## Target Runtime Semantics for This Slice

After this card, runtime crop yield should still use the current non-environmental formula:

```
adjustedQuantity =
  probabilisticRound(
    vanillaProduceQuantity
    * selectedBaseCropMultiplier
    * cropSeasonalFactor
  )
```

It must not yet use:

* biomeYieldFactor;
* climateYieldFactor;
* environmentalFactor;
* CropYieldEffectiveMultiplierCalculator;
* BiomeProfileProvider;
* SeasonProfileProvider.

The only intended behavior change is that the selected multiplier is now a crop-specific base range instead of a biome-scoped range.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: infrastructure, with domain/application changes only if required by the provider contract

Rules:

* crop-yields.yml remains owned by crops/yield.
* Raw YAML must not leak into domain.
* YamlCropYieldPolicyProvider remains infrastructure.
* CropYieldPolicyProvider remains the typed provider port.
* Domain must not import YAML, Bukkit, Paper, file I/O, or infrastructure.
* CropYieldService must not consume upstream profile providers in this card.
* Do not add BiomeProfileProvider to CropYieldService.
* Do not add SeasonProfileProvider to CropYieldService.
* Do not use environmental calculators in runtime in this card.

## Implementation Guidance

Prefer the smallest safe migration.

If CropYieldPolicyProvider currently requires a BiomeId argument, either:

1. keep the port signature unchanged and have YamlCropYieldPolicyProvider return the global crop base policy regardless of BiomeId for this transitional slice; or
2. change the port to expose a biome-independent base crop policy only if the change remains small and all compile-time callers/tests can be updated cleanly.

Do not perform a broad service refactor only to remove a temporary unused BiomeId. If the BiomeId remains temporarily unused by the provider, document that in ARCHITECTURE.md as transitional behavior until environmental runtime wiring is implemented.

## Acceptance Behavior

1. crop-yields.yml no longer contains biome-scoped crop yield multiplier ranges.

2. crop-yields.yml contains crop-specific base multiplier ranges for all currently supported yield crops.

3. crop-yields.yml preserves crop-owned seasonal factors for supported crops and seasons.

4. YamlCropYieldPolicyProvider parses the new base-range shape into the existing typed crop yield policy model, or into the smallest necessary updated model.

5. YamlCropYieldPolicyProvider rejects unsupported crop keys under the new shape.

6. YamlCropYieldPolicyProvider rejects missing base-multiplier ranges for configured crops.

7. YamlCropYieldPolicyProvider rejects invalid multiplier ranges according to existing domain validation.

8. Existing CropYieldService tests still pass after updates.

9. Runtime crop yield still does not consume BiomeProfileProvider or SeasonProfileProvider.

10. Runtime crop yield still does not use biomeYieldFactor, climateYieldFactor, environmentalFactor, or CropYieldEffectiveMultiplierCalculator.

11. ARCHITECTURE.md is updated if the provider contract or transitional behavior changes.

## TDD Plan

1. Inspect existing YamlCropYieldPolicyProvider tests and crop-yields.yml.
2. Add or update infrastructure tests for the new crop-specific base-range YAML shape.
3. Confirm the focused provider tests fail against the legacy parser.
4. Update crop-yields.yml to the new shape.
5. Update YamlCropYieldPolicyProvider and related domain/provider contract only as needed.
6. Update application tests only where the provider contract changed.
7. Run focused tests.
8. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicyProvider.java only if the provider contract must change
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java only if the provider contract must change

Resources:

* java/app/src/main/resources/crop-yields.yml

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProviderTest.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldServiceTest.java only if the provider contract changes

Documentation:

* ARCHITECTURE.md if target structure, implemented behavior, or transitional provider behavior changes.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: YamlCropYieldPolicyProvider

Responsibility:

* Parse crop-yields.yml.
* Translate YAML into typed crop yield domain policy objects.
* Reject malformed or unsupported crop yield configuration.

Must not:

* Resolve biome.
* Read biome profiles.
* Read season profiles.
* Query current season.
* Apply environmental factors.
* Contain crop harvest event logic.

Class: CropYieldService

Responsibility in this card:

* Preserve existing orchestration behavior except for any required provider contract adaptation.
* Continue to apply selected base multiplier and cropSeasonalFactor.
* Continue unsupported fallback behavior.

Must not:

* Consume BiomeProfileProvider.
* Consume SeasonProfileProvider.
* Use CropYieldBiomeFactorCalculator.
* Use CropYieldClimateFactorCalculator.
* Use CropYieldEnvironmentalFactorCalculator.
* Use CropYieldEffectiveMultiplierCalculator.

## Out of Scope

Explicitly forbidden work:

* Adding environmental factor runtime wiring.
* Adding BiomeProfileProvider injection.
* Adding SeasonProfileProvider injection.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Changing PaperCropHarvestListener.
* Changing DynamicBiomes runtime wiring.
* Supporting both legacy and new YAML formats unless the current tests force a very small compatibility shim.
* Adding migration tooling.
* Rebalancing beyond conservative base ranges.
* Adding generic configuration abstractions.
* Adding unrelated crop types.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md conflicts with this card.
* AGENTS.md conflicts with this card.
* the current provider/domain model cannot support a base crop policy without a broad refactor.
* changing the provider contract would require unrelated application or runtime rewrites.
* tests would require unsafe Bukkit/Paper event testing.
* the task would require biome/profile or seasons/profile to own crop-specific yield configuration.
* supporting the new YAML shape would require adding generic YAML infrastructure not justified by duplication.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*YamlCropYieldPolicyProvider*" --no-daemon
cd java && ./gradlew test --tests "*CropYieldServiceTest*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Also inspect:

```
git diff -- java/app/src/main/resources/crop-yields.yml
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. New crop-yields.yml shape.
4. Whether CropYieldPolicyProvider signature changed.
5. Runtime formula after this card.
6. Commands run and results.
7. Confirmation that environmental factors are still not wired.
8. Deferred work.
