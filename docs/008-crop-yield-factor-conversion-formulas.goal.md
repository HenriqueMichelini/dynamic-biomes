---

id: CROP-YIELD-FACTOR-CONVERSION-FORMULAS
type: documentation
bounded_context: crops
capability: yield
layer: docs
status: Ready
expected_commit: "docs(crops): define crop yield factor conversion formulas"
----------------------------------------------------------------------------

# Goal Card: Define Crop Yield Factor Conversion Formulas

## Status

Ready

## Goal

Document exact conversion formulas for deriving Option B crop yield biome and climate factors from existing normalized upstream profile values.

## Why Now

The previous crop yield factor derivation implementation card was correctly blocked because numeric semantics were underspecified.

The existing upstream models expose:

* Fertility normalized in [0.0, 1.0]
* SeasonalAdjustment normalized in [-1.0, 1.0]

The architecture documents derivation sources, but not exact formulas. This card records the formulas so the next domain implementation can proceed without inventing balancing semantics.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* docs/006-crop-yield-factor-derivation-semantics.goal.md
* docs/007-crop-yield-factor-derivation-domain.goal.md
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/biome/profile/domain/Fertility.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonalAdjustment.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/profile/domain/SeasonClimateAdjustment.java

## Current State

* Option B is the selected future crop yield model.
* CropYieldEnvironmentalFactor exists.
* CropYieldEnvironmentalFactorCalculator exists.
* Crop yield factor derivation implementation is blocked because formulas are not documented.
* Runtime crop yield behavior is unchanged.
* YAML resources are unchanged.

## Formula Decision

### biomeYieldFactor

Fertility is normalized in [0.0, 1.0].

Use midpoint-neutral mapping:

```
neutral fertility = 0.5

biomeYieldFactor =
  1.0 + ((fertility - 0.5) * 0.40)
```

Resulting examples:

```
fertility 0.00 -> biomeYieldFactor 0.80
fertility 0.25 -> biomeYieldFactor 0.90
fertility 0.50 -> biomeYieldFactor 1.00
fertility 0.75 -> biomeYieldFactor 1.10
fertility 1.00 -> biomeYieldFactor 1.20
```

Bounds:

```
minimum biomeYieldFactor = 0.80
maximum biomeYieldFactor = 1.20
```

Rationale:

* Fertility 1.0 means highly fertile, not neutral.
* Fertility 0.5 is the neutral midpoint.
* The first version should be conservative.
* The biome factor should be meaningful but not dominate crop-specific tuning.

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

Resulting examples:

```
temperature 0.00, humidity 0.00 -> climateYieldFactor 1.000
temperature 1.00, humidity 1.00 -> climateYieldFactor 1.150
temperature -1.00, humidity -1.00 -> climateYieldFactor 0.850
temperature 1.00, humidity -1.00 -> climateYieldFactor 1.000
temperature 0.50, humidity -0.50 -> climateYieldFactor 1.000
```

Bounds:

```
minimum climateYieldFactor = 0.85
maximum climateYieldFactor = 1.15
```

Rationale:

* Season climate adjustment is delta-like.
* Adjustment 0.0 is neutral.
* Temperature and humidity are averaged to avoid over-amplifying climate influence.
* The climate factor should be milder than explicit cropSeasonalFactor because cropSeasonalFactor remains the crop-specific balancing hook.

## Double-Counting Guardrails

The documentation must state:

* Do not multiply these factors into current runtime crop yield while crop-yields.yml still uses biome-scoped multiplier ranges.
* Do not treat fertility 1.0 as neutral.
* Do not treat SeasonalAdjustment values as direct multipliers.
* Do not push crop-yield factor vocabulary into biome/profile or seasons/profile.
* crops/yield owns the interpretation from environmental values to crop yield behavior.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: documentation

Rules:

* biome/profile remains environmental.
* seasons/profile remains environmental.
* crops/yield owns crop-specific yield interpretation.
* Runtime behavior must not change in this card.
* YAML shape must not change in this card.

## Acceptance Behavior

1. ARCHITECTURE.md documents the exact biomeYieldFactor formula.
2. ARCHITECTURE.md documents fertility 0.5 as neutral.
3. ARCHITECTURE.md documents the biomeYieldFactor range [0.80, 1.20].
4. ARCHITECTURE.md documents the exact climateYieldFactor formula.
5. ARCHITECTURE.md documents SeasonalAdjustment 0.0 as neutral.
6. ARCHITECTURE.md documents the climateYieldFactor range [0.85, 1.15].
7. ARCHITECTURE.md documents the double-counting guardrails.
8. No production code, tests, YAML resources, or runtime wiring are changed.

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
* Changing crop yield domain calculators.
* Adding factor derivation calculators.
* Adding BiomeProfileProvider to CropYieldService.
* Adding SeasonProfileProvider to CropYieldService.
* Changing crop-yields.yml.
* Changing biome-profiles.yml.
* Changing season-profiles.yml.
* Changing runtime wiring.
* Rebalancing runtime crop yield.
* Migrating biome-scoped ranges.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md already documents conflicting formulas.
* Fertility is not normalized in [0.0, 1.0].
* SeasonalAdjustment is not normalized in [-1.0, 1.0].
* documenting these formulas would require production code changes.
* documenting these formulas would require YAML changes.

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
3. Exact formulas recorded.
4. Commands run and results.
5. Confirmation that runtime behavior did not change.
6. Confirmation that YAML resources did not change.
