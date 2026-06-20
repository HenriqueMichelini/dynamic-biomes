---

id: CROP-YIELD-RESTORE-BIOME-SCOPED-RUNTIME
type: infrastructure
bounded_context: crops
capability: yield
layer: infrastructure
status: Ready
expected_commit: "feat(crops): restore biome-scoped crop yield policies"
------------------------------------------------------------------------

# Goal Card: Restore Biome-Scoped Crop Yield Runtime

## Status

Ready

## Goal

Restore crop-yields.yml and YamlCropYieldPolicyProvider to biome-scoped crop yield policy lookup, and update runtime crop yield calculation so biome influence comes from crop-yield-owned biome/crop policy ranges rather than the generic fertility-derived biomeYieldFactor.

## Why Now

ARCHITECTURE.md now documents biome-scoped crop yield policy as the target design.

The previous base-range-only migration made biome influence generic through BiomeProfile fertility. That is valid environmental modeling, but it does not fully match the plugin goal:

```
crop X should be able to have different configured effectiveness in biome Y and biome Z
```

Feature-owned biome-scoped policy is valid. The ore drops capability already follows this pattern: ore-drops.yml is owned by ore/drops and is keyed by BiomeId. Crop yield should follow the same feature-owned biome policy pattern.

This card restores biome-scoped crop yield policy while avoiding double-counting. The active runtime formula should no longer multiply a generic fertility-derived biomeYieldFactor on top of a biome-scoped crop multiplier.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/012-crop-yield-base-range-yaml-migration.goal.md
* docs/017-crop-yield-restore-biome-scoped-policy-semantics.goal.md
* java/app/src/main/resources/ore-drops.yml
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/

## Current State

* crop-yields.yml currently uses top-level crops with crop-specific base multiplier ranges.
* YamlCropYieldPolicyProvider currently parses one global crop base policy and ignores BiomeId.
* CropYieldPolicyProvider still exposes policyFor(BiomeId), but the BiomeId is transitional and unused.
* CropYieldService currently applies:
  selectedBaseCropMultiplier

  * cropSeasonalFactor
  * fertility-derived biomeYieldFactor
  * climateYieldFactor
* Runtime crop yield uses the full Option B constructor through DynamicBiomes.
* ARCHITECTURE.md now marks base-range-only crop yield semantics as superseded/transitional.

## Target Runtime Semantics

The corrected active runtime formula is:

```
adjustedQuantity =
  probabilisticRound(
    vanillaProduceQuantity
      * selectedBiomeCropMultiplier
      * cropSeasonalFactor
      * climateYieldFactor
  )
```

Definitions:

```
selectedBiomeCropMultiplier:
  crop-yield-owned multiplier range selected by resolved BiomeId and CropKind from crop-yields.yml

cropSeasonalFactor:
  crop-yield-owned crop/season tuning from crop-yields.yml

climateYieldFactor:
  crop-yield-owned interpretation of SeasonProfile climate adjustment
```

Not active in this formula:

```
fertility-derived biomeYieldFactor
```

Reason:

```
selectedBiomeCropMultiplier already represents biome influence.
Applying fertility-derived biomeYieldFactor on top would double-count biome influence.
```

## Target crop-yields.yml Shape

Restore a biome-scoped shape owned by crops/yield.

Preferred shape:

```
biomes:
  minecraft:forest:
    crops:
      wheat:
        multiplier:
          min: 1.00
          max: 1.10
        seasonal-factors:
          spring: 1.05
          summer: 1.00
          autumn: 1.00
          winter: 0.90

      carrots:
        multiplier:
          min: 0.95
          max: 1.05
        seasonal-factors:
          spring: 1.00
          summer: 1.05
          autumn: 1.00
          winter: 0.90

  minecraft:desert:
    crops:
      wheat:
        multiplier:
          min: 0.70
          max: 0.90
        seasonal-factors:
          spring: 1.00
          summer: 0.90
          autumn: 1.00
          winter: 1.00
```

Use the existing supported BiomeId, CropKind, and SeasonId values already present in the project. Balancing values may be conservative, but the semantic requirement is that crop multiplier ranges are selected by biome and crop.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Primary layer: infrastructure
* Secondary layer: application if needed to remove fertility-derived biome factor from runtime

Rules:

* crop-yields.yml remains owned by crops/yield.
* YamlCropYieldPolicyProvider remains infrastructure.
* CropYieldPolicyProvider remains a crop-yield-owned domain provider port.
* CropYieldService remains application orchestration.
* CropYieldService may resolve BiomeContext for BiomeId.
* CropYieldService must not import upstream infrastructure.
* Do not move crop-specific fields into biome-profiles.yml.
* Do not move crop-specific fields into season-profiles.yml.
* Do not change biome/profile or seasons/profile domain models.
* Do not change PaperCropHarvestListener behavior.
* Do not add generic effect abstractions.

## Implementation Guidance

Keep CropYieldPolicyProvider.policyFor(BiomeId). It is now correct again.

YamlCropYieldPolicyProvider should:

* require root.biomes;
* validate supported biome keys as BiomeId values;
* validate each biome has a crops map;
* validate supported crop keys under each biome;
* parse each crop multiplier range;
* parse optional or required seasonal-factors according to existing crop yield conventions;
* throw UnsupportedCropYieldPolicyException or the existing configured exception when a biome policy is absent;
* no longer ignore BiomeId.

CropYieldService should:

* resolve BiomeContext;
* use biomeContext.biomeId() to load the biome-scoped crop yield policy;
* use the selected biome/crop multiplier;
* use cropSeasonalFactor;
* use climateYieldFactor from SeasonProfile;
* not apply fertility-derived biomeYieldFactor in active runtime calculation.

DynamicBiomes should:

* stop wiring CropYieldBiomeFactorCalculator if CropYieldService no longer needs it;
* stop wiring CropYieldEnvironmentalFactorCalculator or CropYieldEffectiveMultiplierCalculator if the corrected formula no longer uses them;
* keep SeasonProfileProvider and CropYieldClimateFactorCalculator wired if required by CropYieldService.

Do not delete domain calculators in this card unless they become private dead code and removal is clearly smaller than retaining them. Prefer deferring removal of unused fertility/environmental/effective calculators to a cleanup card if deletion would broaden the diff.

## Acceptance Behavior

1. crop-yields.yml uses top-level biomes and no longer uses the base-range-only top-level crops shape.

2. YamlCropYieldPolicyProvider parses crop yield policies by BiomeId.

3. YamlCropYieldPolicyProvider no longer ignores the BiomeId supplied to policyFor(BiomeId).

4. Given two biomes with different wheat multiplier ranges, policyFor(firstBiome) and policyFor(secondBiome) return policies with different wheat rules.

5. Given an unsupported or missing biome policy, CropYieldService preserves vanilla produce quantity through the existing unsupported policy fallback.

6. CropYieldService applies selectedBiomeCropMultiplier and cropSeasonalFactor.

7. CropYieldService applies climateYieldFactor from SeasonProfile climate adjustment.

8. CropYieldService does not apply fertility-derived biomeYieldFactor in active runtime calculation.

9. CropYieldService no longer requires BiomeProfile fertility for crop yield runtime behavior beyond the resolved BiomeId.

10. DynamicBiomes wiring is updated only as needed to match the corrected CropYieldService constructor.

11. PaperCropHarvestListener remains behaviorally unchanged.

12. biome-profiles.yml and season-profiles.yml are unchanged.

13. ARCHITECTURE.md is updated if implementation details differ from the documented corrected target.

## TDD Plan

1. Update YamlCropYieldPolicyProvider tests first:

   * valid biome-scoped shape loads;
   * two biomes can produce different crop rules;
   * missing root.biomes fails;
   * missing biome policy produces the existing unsupported policy behavior;
   * unsupported crop keys still fail.

2. Confirm provider tests fail against the current base-range parser.

3. Update crop-yields.yml to the restored biome-scoped shape.

4. Update YamlCropYieldPolicyProvider to parse root.biomes and use BiomeId.

5. Update CropYieldService tests:

   * biome-specific policy affects result;
   * climate factor still affects result;
   * fertility-derived biome factor is no longer applied;
   * unsupported biome/policy fallback still preserves vanilla quantity.

6. Update CropYieldService and constructor dependencies as needed.

7. Update DynamicBiomes composition only if CropYieldService constructor changes.

8. Run focused provider and service tests.

9. Run full verification.

## Expected Files

Production:

* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java only if constructor wiring changes

Resources:

* java/app/src/main/resources/crop-yields.yml

Tests:

* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProviderTest.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldServiceTest.java
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/PluginResourcesTest.java if resource shape characterization exists
* java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/PaperCropHarvestListenerTest.java only if service construction fixtures break

Documentation:

* ARCHITECTURE.md if needed

Not expected:

* biome/profile production files
* seasons/profile production files
* biome-profiles.yml
* season-profiles.yml
* PaperCropHarvestListener production behavior changes

## Responsibility / Collaboration Notes

Class: YamlCropYieldPolicyProvider

Responsibility:

* Parse crop-yields.yml.
* Load crop-yield-owned biome/crop policies.
* Select policy by BiomeId.
* Reject malformed crop yield configuration.

Must not:

* Read biome-profiles.yml.
* Read season-profiles.yml.
* Derive environmental factors.
* Resolve Bukkit biomes.

Class: CropYieldService

Responsibility:

* Resolve BiomeContext.
* Use BiomeId for crop yield policy lookup.
* Read current SeasonId.
* Load current SeasonProfile.
* Apply biome-scoped crop multiplier, crop seasonal factor, and climate factor.
* Preserve vanilla fallback for unsupported biome or unsupported crop yield policy.

Must not:

* Apply fertility-derived biomeYieldFactor while biome-scoped crop multipliers are active.
* Parse YAML.
* Import upstream infrastructure.
* Contain Bukkit event logic.

Class: DynamicBiomes

Responsibility:

* Wire the corrected CropYieldService dependencies.
* Keep composition thin.

Must not:

* Calculate crop yield factors directly.
* Parse crop-yields.yml manually.
* Contain crop yield business logic.

## Out of Scope

Explicitly forbidden work:

* Removing CropYieldPolicyProvider.policyFor(BiomeId).
* Removing biome-scoped crop yield policy semantics.
* Moving crop yield rules into biome-profiles.yml.
* Moving crop yield rules into season-profiles.yml.
* Changing biome/profile domain.
* Changing seasons/profile domain.
* Changing PaperCropHarvestListener behavior.
* Adding crop environmental sensitivity.
* Adding config migration tooling.
* Supporting both base-range-only and biome-scoped YAML shapes unless existing tests force a very small temporary compatibility shim.
* Rebalancing beyond conservative biome/crop values.
* Adding diagnostics, reload commands, or presentation output.
* Deleting domain calculators unless deletion is trivial and clearly not broader than retaining them.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md does not reflect biome-scoped crop yield policy as the corrected target.
* restoring biome-scoped crop policy requires crop-specific fields in biome/profile.
* the current CropYieldPolicy domain model cannot represent biome-scoped policies without broad refactoring.
* changing CropYieldService would require upstream infrastructure imports.
* implementing this requires Bukkit/Paper event simulation.
* the task would require changing unrelated crop growth behavior.
* the task would require supporting legacy and new YAML shapes in the same provider.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*YamlCropYieldPolicyProvider*" --no-daemon
cd java && ./gradlew test --tests "*CropYieldServiceTest*" --no-daemon
cd java && ./gradlew test --tests "*PluginResourcesTest*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Also inspect:

```
git diff -- java/app/src/main/resources/crop-yields.yml
git diff -- java/app/src/main/resources/biome-profiles.yml
git diff -- java/app/src/main/resources/season-profiles.yml
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
git grep -n "new CropYieldService" -- java/app/src/main/java java/app/src/test/java
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. New crop-yields.yml shape.
3. Whether CropYieldPolicyProvider.policyFor(BiomeId) now uses BiomeId again.
4. Exact runtime formula after this card.
5. Whether fertility-derived biomeYieldFactor is removed from active runtime.
6. Whether climateYieldFactor remains active.
7. Commands run and results.
8. Confirmation that biome-profiles.yml and season-profiles.yml did not change.
9. Deferred cleanup work.
