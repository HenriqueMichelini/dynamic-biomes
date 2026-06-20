---
id: CROP-PERF-0002
type: architecture
bounded_context: crops
capability: performance
layer: docs
status: Ready
expected_commit: "docs(crops): define crop environmental state composition"
---

# Goal Card: Define Crop-Owned CropEnvironmentalState Composition

## Goal

Specify v1 environmental composition for crop performance.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0001-reconcile-crop-environmental-design-in-architecture.goal.md`
- `docs/implementation/0002-define-crop-owned-crop-environmental-state-composition.goal.md`

## Current-State Problem

There is no crop-side read model for environmental variables, and season handling could be misread as modifying all variables.

## Target-State Outcome

V1 composition is unambiguous:

- `CropEnvironmentalState` is a crop-side read model of normalized `0.0..1.0` variables.
- Variables are wind speed, rain strength, humidity, temperature, solar incidence, and soil fertility.
- V1 uses biome humidity and temperature, optionally modified by season multiplicatively.
- V1 soil fertility comes only from `BiomeProfile.Fertility`.
- Do not apply season or weather factors to soil fertility in v1.
- Wind speed, rain strength, and solar incidence default neutral until upstream contracts exist.
- Use `finalVariable = clamp01(biomeVariable * seasonVariableFactor)` only for season-modified variables.

## Files Or Areas Likely Affected

- `ARCHITECTURE.md`
- Later implementation under `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance`
- Matching tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance`

## OOP / DDD / TDD Guardrails

- Keep `CropEnvironmentalState` documented as a crop-owned read model, not a biome or season model.
- Do not move crop-specific behavior or terminology into `biome` or `seasons`.
- Document variable ownership and composition rules in terms of typed domain concepts, not raw YAML or Bukkit/Paper state.
- This is a docs/design card. Later implementation must use TDD, but this card must not add production or test code.

## Implementation Boundaries

- This card is documentation/design clarification only unless a future implementation card explicitly selects code changes.
- Do not introduce weather, local soil persistence, fertilizer, irrigation, farmland degradation, block-level fertility, or variable interactions.
- Do not apply season or weather factors to soil fertility in v1.
- Do not change growth, yield, resource YAML, runtime wiring, or command output.

## Test/Verification Expectations

- For the documentation card, run `git diff --check`.
- Later implementation tests should cover `finalVariable = clamp01(biomeVariable * seasonVariableFactor)` for humidity and temperature only.
- Later implementation tests should cover neutral defaults for wind speed, rain strength, and solar incidence.
- Later implementation tests should cover biome-only soil fertility.

## Dependencies

- Card 1: Reconcile Crop Environmental Design In Architecture.

## Risks Or Migration Notes

- This card prevents accidental soil fertility double-counting or hidden local-soil modeling.
- The neutral defaults are temporary until upstream environmental contracts exist.

## Acceptance Behavior

1. Given the crop environmental design, when this card is complete, then every v1 variable source is documented.
2. Given season-modified variables, when this card is complete, then the only documented formula is `finalVariable = clamp01(biomeVariable * seasonVariableFactor)`.
3. Given soil fertility, when this card is complete, then no season or weather factor applies to it in v1.

## Stop Conditions

Stop and report if the implementation would require adding upstream weather contracts, local soil state, runtime wiring, or any code beyond architecture documentation.
