---

id: CROP-YIELD-RESTORE-BIOME-SCOPED-POLICY-SEMANTICS
type: documentation
bounded_context: crops
capability: yield
layer: docs
status: Ready
expected_commit: "docs(crops): restore biome-scoped crop yield policy semantics"
--------------------------------------------------------------------------------

# Goal Card: Restore Biome-Scoped Crop Yield Policy Semantics

## Status

Ready

## Goal

Update the crop yield design documentation to restore explicit biome-scoped crop yield policy semantics, matching the plugin goal that different crops can behave differently across different biomes.

## Why Now

The recent Option B work migrated crop-yields.yml from biome-scoped crop yield ranges to crop-specific base multiplier ranges and introduced a generic biomeYieldFactor derived from BiomeProfile fertility.

That made biome influence environmental and generic, but it weakened an important gameplay goal:

```
crop X should be able to have different configured effectiveness in biome Y and biome Z
```

The ore drops capability already uses feature-owned biome-scoped policy configuration. That pattern is valid because ore-drops.yml belongs to ore/drops, not biome/profile.

By the same reasoning, crop-yields.yml may also be biome-scoped as long as the policy remains owned by crops/yield and crop-specific behavior is not moved into biome/profile.

This card documents the corrected direction before any implementation changes.

## Read First

* AGENTS.md
* ARCHITECTURE.md
* java/app/src/main/resources/ore-drops.yml
* java/app/src/main/resources/crop-yields.yml
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain/OreDropPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/CropYieldPolicyProvider.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java
* java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/infrastructure/YamlCropYieldPolicyProvider.java

## Current State

* Runtime crop yield currently uses Option B wiring.
* crop-yields.yml currently uses crop-specific base multiplier ranges.
* YamlCropYieldPolicyProvider currently parses one global crop base policy.
* CropYieldPolicyProvider still exposes policyFor(BiomeId), but the provider ignores the supplied BiomeId.
* CropYieldService resolves BiomeContext and derives a generic biomeYieldFactor from BiomeProfile fertility.
* CropYieldService also derives climateYieldFactor from SeasonProfile climate adjustment.
* A pending cleanup card proposes removing BiomeId from CropYieldPolicyProvider.

## Corrected Design Decision

Do not remove biome-scoped crop yield policy semantics.

The corrected target model is:

```
effectiveMultiplier =
  selectedBiomeCropMultiplier
  * cropSeasonalFactor
  * climateYieldFactor
```

Definitions:

```
selectedBiomeCropMultiplier:
  crop-yield-owned configured multiplier range selected by resolved BiomeId and CropKind

cropSeasonalFactor:
  crop-yield-owned crop-specific seasonal factor from crop-yields.yml

climateYieldFactor:
  crop-yield-owned interpretation of published SeasonProfile climate adjustment
```

The generic fertility-derived biomeYieldFactor should not be applied together with selectedBiomeCropMultiplier by default, because that would double-count biome influence.

Biome influence should primarily come from crop-yields.yml biome/crop policy rules, just as ore influence comes from ore-drops.yml biome/ore policy rules.

## Explicitly Superseded Direction

This card supersedes the pending cleanup direction:

```
remove BiomeId from CropYieldPolicyProvider.policyFor(BiomeId)
```

Do not implement that cleanup while biome-scoped crop yield policy is the target design.

CropYieldPolicyProvider should continue to accept BiomeId or should otherwise expose a biome-scoped policy lookup method.

## Valid Feature-Owned Biome Configuration

The architecture must clarify this distinction:

Valid:

```
crop-yields.yml contains crop-specific yield policy keyed by BiomeId
ore-drops.yml contains ore-specific drop policy keyed by BiomeId
```

Invalid:

```
biome-profiles.yml contains cropYieldMultiplier
biome-profiles.yml contains oreDropMultiplier
biome/profile domain owns crop-specific or ore-specific behavior
```

Feature contexts may key their own policies by BiomeId. Upstream biome/profile remains environmental and must not own feature-specific behavior.

## Target crop-yields.yml Semantics

Document the preferred target shape.

Example shape:

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

Notes:

* Exact balancing values are implementation/balancing work and do not need to be finalized in this documentation card.
* The important semantic decision is that the crop yield multiplier range is biome-scoped and crop-specific.
* Climate influence may remain derived from season-profiles.yml.
* Generic fertility-derived biomeYieldFactor is not part of the default runtime formula when biome-scoped crop multipliers are active.

## Relationship to BiomeProfile Fertility

BiomeProfile fertility remains valid environmental data.

However, in the corrected crop yield runtime formula, fertility-derived biomeYieldFactor should be one of these:

1. Removed from active crop yield runtime; or
2. Neutralized by default; or
3. Deferred into a later explicit crop environmental sensitivity model.

Do not apply fertility-derived biomeYieldFactor on top of selectedBiomeCropMultiplier without a separate design card, because both represent biome influence.

A future model may add crop-specific environmental sensitivity, for example:

```
effectiveMultiplier =
  selectedBiomeCropMultiplier
  * cropEnvironmentalSensitivityFactor
  * cropSeasonalFactor
  * climateYieldFactor
```

But that is out of scope for this card.

## Architectural Boundary

* Bounded context: crops
* Capability: yield
* Layer: documentation

Rules:

* crops/yield owns crop-specific yield policy.
* crop-yields.yml may be keyed by BiomeId because it belongs to crops/yield.
* biome/profile remains environmental and must not gain crop-specific yield fields.
* seasons/profile remains environmental and must not gain crop-specific yield fields.
* CropYieldService may consume published upstream domain contracts.
* CropYieldService must not import upstream infrastructure.
* Runtime behavior must not change in this card.
* YAML resources must not change in this card.

## Acceptance Behavior

1. ARCHITECTURE.md states that biome-scoped crop yield policy is valid and selected as the target crop yield design.

2. ARCHITECTURE.md distinguishes feature-owned biome-keyed policy from forbidden biome-owned feature behavior.

3. ARCHITECTURE.md updates the target crop yield formula to:

   ```
   selectedBiomeCropMultiplier
     * cropSeasonalFactor
     * climateYieldFactor
   ```

4. ARCHITECTURE.md states that fertility-derived biomeYieldFactor must not be applied on top of biome-scoped crop multipliers by default.

5. ARCHITECTURE.md states that CropYieldPolicyProvider should remain biome-aware while crop yield policy is biome-scoped.

6. ARCHITECTURE.md marks the previous base-range-only crop-yields.yml direction as superseded or transitional, not the final target.

7. ARCHITECTURE.md marks cleanup of CropYieldPolicyProvider.policyFor(BiomeId) as cancelled/deferred, not ready.

8. No production code is changed.

9. No tests are changed.

10. No YAML resources are changed.

11. Runtime crop yield behavior is unchanged by this card.

## Expected Files

Documentation:

* ARCHITECTURE.md

Production:

* None.

Tests:

* None.

Resources:

* None.

Goal cards:

* docs/017-crop-yield-remove-biome-policy-parameter.goal.md only if the repository tracks goal-card status and it should be marked Deferred or superseded. Do not edit it if goal cards are operational scratch files rather than tracked documentation.

## Out of Scope

Explicitly forbidden work:

* Changing crop-yields.yml.
* Changing YamlCropYieldPolicyProvider.
* Changing CropYieldPolicyProvider.
* Changing CropYieldService.
* Changing CropYieldService tests.
* Changing PaperCropHarvestListener.
* Changing DynamicBiomes.
* Removing fertility-derived calculators.
* Removing climate factor calculators.
* Rebalancing crop yield values.
* Implementing crop environmental sensitivity.
* Supporting both YAML shapes.
* Adding migration tooling.
* Adding diagnostics or commands.

## Stop Conditions

Stop and report instead of editing if:

* ARCHITECTURE.md already documents an explicit decision that crop yield must be base-range-only and biome influence must only come from BiomeProfile fertility.
* documenting biome-scoped crop yield policy would require moving crop-specific behavior into biome/profile.
* documenting this correction would require production code changes.
* repository policy says goal cards must not be revised or superseded through ARCHITECTURE.md.
* the current worktree has uncommitted implementation changes that make the documentation correction ambiguous.

## Verification

Run:

```
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Also inspect:

```
git diff -- ARCHITECTURE.md
git diff -- java/app/src/main/java
git diff -- java/app/src/main/resources
git diff -- java/app/src/test/java
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Sections of ARCHITECTURE.md updated.
3. Whether the pending BiomeId-removal cleanup was marked superseded/deferred.
4. Confirmation that runtime behavior did not change.
5. Confirmation that YAML resources did not change.
6. Commands run and results.
7. Recommended next implementation card.
