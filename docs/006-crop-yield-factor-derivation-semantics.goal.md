---

id: CROP-YIELD-FACTOR-DERIVATION-SEMANTICS
type: documentation
bounded_context: crops
capability: yield
layer: docs
status: Ready
expected_commit: "docs(crops): define crop yield factor derivation semantics"
-----------------------------------------------------------------------------

# Goal Card: Define Crop Yield Factor Derivation Semantics

## Status

Ready

## Goal

Document how Option B crop yield will derive biomeYieldFactor and climateYieldFactor before any runtime wiring or YAML migration occurs.

## Why Now

The crop yield environmental factor value object and calculator now exist, but they only combine already-derived factors. The project still needs a documented rule for deriving:

```
biomeYieldFactor
climateYieldFactor
```

without leaking crop-specific behavior into biome or season contexts.

This card prevents arbitrary formula choices when the next implementation slice starts consuming BiomeProfileProvider and SeasonProfileProvider.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/004-crop-yield-option-b-semantics.goal.md
* docs/005-crop-yield-environmental-factor-domain.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactor.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldEnvironmentalFactorCalculator.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/profile/domain/
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/
* java/app/src/main/resources/biome-profiles.yml
* java/app/src/main/resources/season-profiles.yml
* java/app/src/main/resources/crop-yields.yml

## Current State

* Option B is selected as the future crop yield model:

  effectiveMultiplier =
  selectedBaseCropMultiplier
  * biomeYieldFactor
  * cropSeasonalFactor
  * climateYieldFactor

* CropYieldEnvironmentalFactor and CropYieldEnvironmentalFactorCalculator exist in crops/yield/domain.

* Runtime crop harvest behavior is unchanged.

* crop-yields.yml still uses biome-scoped multiplier ranges.

* CropYieldService still does not consume SeasonProfileProvider or BiomeProfileProvider.

* The formula derivation for biomeYieldFactor and climateYieldFactor is not yet specified.

## Design Decision To Record

Document these semantics in ARCHITECTURE.md.

### biomeYieldFactor

biomeYieldFactor is crop-yield-owned interpretation of published biome profile data.

It may be derived from environmental values such as:

* Fertility
* Humidity
* Temperature
* EcologicalPressure

The preferred first version should be conservative:

```
biomeYieldFactor derives primarily from Fertility.
```

Rationale:

* Fertility maps most directly to crop yield.
* Humidity and temperature can also affect yield, but they overlap with climateYieldFactor if used carelessly.
* EcologicalPressure may be useful later for disease/pest/death systems, but should not be introduced into crop yield unless there is an explicit balancing reason.

### climateYieldFactor

climateYieldFactor is crop-yield-owned interpretation of published season profile climate adjustment data.

It may be derived from environmental adjustments such as:

* Temperature adjustment
* Humidity adjustment

The preferred first version should be conservative:

```
climateYieldFactor derives from season climate temperature and humidity adjustment only.
```

Rationale:

* Season profile data should remain environmental.
* Crop yield owns the interpretation.
* Season profile must not define crop-specific effects.

### cropSeasonalFactor

cropSeasonalFactor remains crop-yield-owned crop-specific seasonal tuning from crop-yields.yml.

It is intentionally separate from climateYieldFactor.

Rationale:

* climateYieldFactor models environmental climate effects.
* cropSeasonalFactor models crop-specific balancing or sensitivity for a named season.

### Double Counting Rule

The documentation must explicitly warn against double-counting the same environmental dimension.

Examples:

* If biomeYieldFactor uses humidity strongly, climateYieldFactor should not also apply a large humidity penalty without balancing.
* If cropSeasonalFactor already encodes harsh winter penalties for wheat, climateYieldFactor should initially be mild.
* Existing biome-scoped crop-yields.yml ranges must not be multiplied by biomeYieldFactor until those ranges are migrated to base crop ranges.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: documentation

Rules:

* crops/yield owns crop-specific yield interpretation.
* biome/profile owns environmental biome profile data only.
* seasons/profile owns environmental season profile data only.
* biome/profile must not gain crop-yield vocabulary.
* seasons/profile must not gain crop-yield vocabulary.
* Downstream crop yield may consume published upstream domain contracts.
* Downstream crop yield must not consume upstream infrastructure.

## Acceptance Behavior

1. ARCHITECTURE.md documents how biomeYieldFactor should be derived in the first Option B implementation.
2. ARCHITECTURE.md documents how climateYieldFactor should be derived in the first Option B implementation.
3. ARCHITECTURE.md states that cropSeasonalFactor remains separate from climateYieldFactor.
4. ARCHITECTURE.md warns against double-counting biome, climate, and seasonal influence.
5. ARCHITECTURE.md states that runtime behavior remains unchanged by this documentation card.
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

* Changing CropYieldService.
* Changing CropYieldEnvironmentalFactor.
* Changing CropYieldEnvironmentalFactorCalculator.
* Adding a biome factor derivation calculator.
* Adding a climate factor derivation calculator.
* Adding SeasonProfileProvider to CropYieldService.
* Adding BiomeProfileProvider to CropYieldService.
* Changing crop-yields.yml.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Changing runtime wiring.
* Rebalancing crop yield values.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md already documents conflicting semantics.
* the existing biome or season profile model does not expose enough data to support the proposed derivation direction.
* documenting the semantics would require changing production code.
* documenting the semantics would require adding crop-specific vocabulary to biome or season contexts.
* the repository has a more specific design-decision document that should own this decision instead of ARCHITECTURE.md.

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
3. Commands run and results.
4. Confirmation that runtime behavior did not change.
5. Confirmation that no YAML shape changed.
6. Deferred implementation cards.
