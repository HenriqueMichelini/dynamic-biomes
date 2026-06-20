---
id: CROP-PERF-0001
type: architecture
bounded_context: crops
capability: performance
layer: docs
status: Ready
expected_commit: "docs(crops): reconcile crop environmental performance architecture"
---

# Goal Card: Reconcile Crop Environmental Design In Architecture

## Goal

Update architecture documentation to define `crops/performance` as the crop-owned environmental interpretation capability.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0001-reconcile-crop-environmental-design-in-architecture.goal.md`

## Current-State Problem

`ARCHITECTURE.md` documents crop growth and crop yield behavior, but the current crop environmental target is still framed around biome-scoped crop yield policy instead of a crop-owned performance capability.

## Target-State Outcome

Architecture clearly separates upstream environmental contributors from crop-owned environmental state, crop profiles, and performance factors.

The documentation must preserve these domain decisions:

- `crops/performance` owns crop-specific interpretation.
- `biome`, `seasons`, and future weather remain upstream environmental contributors.
- `CropEnvironmentalState` is a crop-side read model of normalized `0.0..1.0` variables.
- Variables are wind speed, rain strength, humidity, temperature, solar incidence, and soil fertility.
- `crop-growth.yml` and `crop-yields.yml` remain transitional for one phase.
- `crop-profiles.yml` becomes the crop-owned preference/profile source.
- Biome-scoped crop yield multipliers are transitional once crop performance exists.
- No quality tier, quality probability, quality roll, item quality, quality diagnostics, or `/dynamicbiomes inspect` quality output.

## Files Or Areas Likely Affected

- `ARCHITECTURE.md`

## OOP / DDD / TDD Guardrails

- Treat `ARCHITECTURE.md` as the source of truth for package boundaries and layer responsibilities.
- Preserve the DDD direction: upstream environmental contexts describe conditions; downstream crop contexts interpret them.
- Document object responsibilities only when they are required by the selected crop-performance design; do not introduce speculative abstractions.
- This is a docs-only card, so TDD does not apply to production behavior. Do not add or change code/tests to make the documentation pass.

## Implementation Boundaries

- Docs only.
- Do not modify production Java code.
- Do not modify tests.
- Do not modify YAML resources.
- Do not add providers, services, listeners, runtime wiring, or package scaffolding.
- Do not introduce crop-specific vocabulary into `biome` or `seasons`.
- Do not introduce quality concepts.

## Test/Verification Expectations

- Run `git diff --check`.
- Inspect `git diff --name-status` and stop if production Java, tests, or YAML resources changed.

## Dependencies

- None.

## Risks Or Migration Notes

- This card sets the architectural direction for the following cards.
- The documentation must make it clear that `biome`, `seasons`, and future weather describe environmental conditions; `crops/performance` interprets those conditions for crop behavior.

## Acceptance Behavior

1. Given `ARCHITECTURE.md`, when the card is complete, then it explicitly names `crops/performance` as the crop-owned environmental interpretation capability.
2. Given upstream contexts, when the card is complete, then `biome`, `seasons`, and future weather remain environmental contributors and do not gain crop vocabulary.
3. Given crop performance output, when the card is complete, then quality-related output remains explicitly out of scope.

## Stop Conditions

Stop and report instead of editing if the architecture update would require changing production code, tests, YAML resources, or runtime behavior in the same card.
