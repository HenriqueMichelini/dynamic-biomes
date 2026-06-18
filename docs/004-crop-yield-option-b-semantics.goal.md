---
id: CROP-YIELD-OPTION-B-SEMANTICS
type: documentation
bounded_context: crops
capability: yield
layer: docs
status: Ready
expected_commit: "docs(crops): record option b crop yield multiplier semantics"
---

# Goal Card: Record Option B Crop Yield Multiplier Semantics

## Status

Ready

## Goal

Document that crop yield will move toward base crop multiplier ranges plus explicit environmental factors, avoiding double-counted biome influence.

## Why Now

The current implementation uses biome-scoped crop yield multiplier ranges from `crop-yields.yml`. The selected future design is Option B, where crop yield uses base crop ranges plus separate biome and climate factors. This decision must be documented before implementation so later code and YAML changes have clear ownership semantics.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/003-crop-yield-profile-isolation.goal.md`
- `java/app/src/main/resources/crop-yields.yml`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application/CropYieldService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain/`

## Current State

- Crop yield currently uses `crop-yields.yml`.
- Current multiplier ranges are biome-scoped.
- Crop yield applies crop-owned seasonal factors from `crop-yields.yml`.
- Crop yield does not currently consume `SeasonProfileProvider`.
- Crop yield does not currently consume `BiomeProfileProvider`.
- Characterization tests now document this current behavior.

## Target Semantics

Future crop yield should use:

```text
effectiveMultiplier =
  selectedBaseCropMultiplier
  * biomeYieldFactor
  * cropSeasonalFactor
  * climateYieldFactor
```

Definitions:

```text
selectedBaseCropMultiplier:
  crop-specific configured base range, no longer biome-tuned

biomeYieldFactor:
  explicit environmental/biome contribution, derived from biome profile data or crop-yield-owned biome factor configuration

cropSeasonalFactor:
  crop-specific seasonal sensitivity or override from crop-yields.yml

climateYieldFactor:
  derived from season-profiles.yml climate-adjustment, initially temperature/humidity unless the repository already exposes a better existing model
```

## Architectural Boundary
- Bounded context: `crops`
- Capability: `yield`
- Layer: documentation
Rules:
- `biome` must remain environmental and must not gain crop-specific vocabulary.
- `seasons` must remain environmental and must not gain crop-specific vocabulary.
- `crop-yields.yml` remains owned by `crops/yield`.
- `biome-profiles.yml` remains owned by `biome/profile`.
- `season-profiles.yml` remains owned by `seasons/profile`.
- Downstream crop yield code may consume published biome/season domain contracts, but not upstream infrastructure.
- Do not change production code in this card.
- Do not change YAML shape in this card.

## Acceptance Behavior
1. `ARCHITECTURE.md` records Option B as the selected future crop yield design.
2. The documentation explicitly warns that current biome-scoped crop yield ranges must not be multiplied by a new `biomeYieldFactor`.
3. The documentation states that current biome-scoped ranges are legacy/current behavior until a migration card changes `crop-yields.yml`.
4. The documentation states that runtime behavior is unchanged by this card.
5. The documentation keeps ownership clear:
   - crop-specific yield policy belongs to `crops/yield`;
   - biome profile data belongs to `biome/profile`;
   - season profile data belongs to `seasons/profile`.

## Expected Files

Production:

- None.

Tests:

- None.

Documentation:

- `ARCHITECTURE.md`

## Out of Scope

Explicitly forbidden work:

- Modifying CropYieldService.
- Adding SeasonProfileProvider to crop yield runtime.
- Adding BiomeProfileProvider to crop yield runtime.
- Adding a new calculator.
- Changing crop-yields.yml.
- Migrating YAML shape.
- Rebalancing crop yield values.
- Changing listener behavior.
- Changing runtime wiring.

## Stop Conditions

Stop and report instead of editing if:

- ARCHITECTURE.md already documents a conflicting target model.
- the selected model would require biome or season contexts to own crop-specific rule vocabulary;
- documenting the model would require production code changes;
- the repository has a different explicit documentation location for feature design decisions.

## Verification

Run:
```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

## Report Back

After implementation, report:

1. Files changed.
2. Documentation section updated.
3. Commands run and results.
4. Whether runtime behavior changed.
5. Deferred implementation cards.
