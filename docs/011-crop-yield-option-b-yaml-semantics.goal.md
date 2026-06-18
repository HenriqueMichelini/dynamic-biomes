---

id: CROP-YIELD-OPTION-B-YAML-SEMANTICS
type: documentation
bounded_context: crops
capability: yield
layer: docs
status: Ready
expected_commit: "docs(crops): define option b crop yield yaml semantics"
-------------------------------------------------------------------------

# Goal Card: Define Option B Crop Yield YAML Semantics

## Status

Ready

## Goal

Document the target crop-yields.yml shape and migration semantics for Option B before changing YAML resources, provider parsing, or runtime crop harvest behavior.

## Why Now

The Option B domain math is now modeled:

```
biomeYieldFactor
climateYieldFactor
environmentalFactor
effectiveMultiplier =
  selectedBaseCropMultiplier
  * cropSeasonalFactor
  * environmentalFactor
```

However, crop-yields.yml still uses biome-scoped multiplier ranges. Runtime wiring must not happen while those ranges remain biome-tuned, because that would double-count biome influence.

This card defines the future configuration shape before implementation.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* docs/008-crop-yield-factor-conversion-formulas.goal.md
* docs/009-crop-yield-factor-derivation-domain.goal.md
* docs/010-crop-yield-option-b-multiplier-domain.goal.md
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java

## Current State

* crop-yields.yml currently stores crop yield multiplier ranges under biomes.
* Those ranges are current/legacy biome-scoped ranges.
* CropYieldService currently resolves biome and asks CropYieldPolicyProvider for a policy.
* CropYieldService does not consume BiomeProfileProvider or SeasonProfileProvider.
* New Option B domain calculators exist but are not wired.
* Runtime crop harvest behavior is unchanged.

## Target Configuration Semantics

Document that Option B crop-yields.yml should move from biome-scoped multiplier ranges to crop base ranges.

Preferred target semantics:

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
```

Notes:

* The exact numeric values may be placeholders if the current resource uses different balancing values.
* The important semantic change is that the multiplier range becomes crop-specific base range, not biome-specific range.
* Biome influence comes from biomeYieldFactor derived from BiomeProfile fertility.
* Climate influence comes from climateYieldFactor derived from SeasonProfile climate adjustment.
* Crop-specific seasonal tuning remains in crop-yields.yml as cropSeasonalFactor.

## Legacy Configuration Semantics

Document that the current biomes-based crop-yields.yml shape is legacy/current behavior.

The documentation must state:

* Current biome-scoped ranges must not be multiplied by biomeYieldFactor.
* Option B runtime wiring requires the base crop range shape first.
* The project does not need to support both shapes long-term unless explicitly chosen.
* If backward compatibility is desired later, it must be a separate card with explicit migration behavior.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: documentation

Rules:

* crop-yields.yml remains owned by crops/yield.
* biome-profiles.yml remains owned by biome/profile.
* season-profiles.yml remains owned by seasons/profile.
* biome/profile must not gain crop-yield-specific configuration.
* seasons/profile must not gain crop-yield-specific configuration.
* This card must not change runtime behavior.
* This card must not change YAML files.
* This card must not change provider parsing.

## Acceptance Behavior

1. ARCHITECTURE.md documents the target Option B crop-yields.yml semantics.
2. ARCHITECTURE.md states that multiplier ranges become crop-specific base ranges.
3. ARCHITECTURE.md states that current biome-scoped ranges are legacy/current behavior.
4. ARCHITECTURE.md explicitly warns that current biome-scoped ranges must not be multiplied by biomeYieldFactor.
5. ARCHITECTURE.md states that YAML/resource migration and provider parsing are deferred implementation work.
6. No production code, tests, YAML resources, or runtime wiring are changed.

## Expected Files

Documentation:

* ARCHITECTURE.md

Production:

* None.

Tests:

* None.

Resources:

* None.

## Out of Scope

Explicitly forbidden work:

* Changing crop-yields.yml.
* Changing YamlCropYieldPolicyProvider.
* Changing CropYieldPolicy.
* Changing CropYieldPolicyProvider.
* Changing CropYieldService.
* Adding BiomeProfileProvider to runtime crop yield.
* Adding SeasonProfileProvider to runtime crop yield.
* Changing PaperCropHarvestListener.
* Changing DynamicBiomes runtime wiring.
* Rebalancing crop yield values.
* Supporting dual legacy/new YAML formats.
* Adding migration tooling.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md already documents conflicting crop-yields.yml semantics.
* the repository has a more specific configuration design document that should own this decision.
* documenting the target shape would require changing production code.
* documenting the target shape would require changing YAML resources.
* the proposed shape would move crop-specific yield configuration into biome or season contexts.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Documentation section updated.
3. Target crop-yields.yml semantics recorded.
4. Commands run and results.
5. Confirmation that runtime behavior did not change.
6. Confirmation that YAML resources did not change.
7. Deferred implementation cards.
