---
id: CROP-PERF-0004
type: infrastructure
bounded_context: crops
capability: performance
layer: infrastructure
status: Ready
expected_commit: "feat(crops): load crop performance profiles from yaml"
---

# Goal Card: Add Crop Performance Profile Provider And YAML Adapter

## Goal

Introduce crop-owned preference profiles loaded from `crop-profiles.yml`.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0003a-add-crop-environmental-state-domain.goal.md`
- `docs/implementation/0003b-add-crop-performance-profile-domain.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- Existing YAML provider tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes`

## Current-State Problem

Existing crop configs describe growth chances and yield multipliers, not crop preferred environmental conditions.

## Target-State Outcome

A typed provider port and YAML adapter load crop preference profiles for supported `CropKind` values.

Required domain decisions:

- `crop-profiles.yml` becomes the crop-owned preference/profile source.
- `crop-growth.yml` and `crop-yields.yml` remain transitional for one phase.
- Missing crop profile / unsupported crop-performance profile is exposed as an unsupported crop-performance condition; neutral result conversion belongs to Card 5B.
- Malformed config, invalid normalized values, duplicate keys, I/O failures, and programming errors still propagate.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/infrastructure`
- `java/app/src/main/resources/crop-profiles.yml`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/infrastructure`
- `java/app/build.gradle.kts` only if needed for resource packaging tests.

## OOP / DDD / TDD Guardrails

- The provider port belongs in `crops/performance/domain`; the YAML adapter and raw YAML parsing belong only in `crops/performance/infrastructure`.
- Keep the provider responsibility typed and crop-performance-specific; do not introduce a generic configuration provider.
- The provider reports supported or unsupported profile data; neutral result conversion belongs to the application service in Card 5B.
- Model YAML translation as adapter code that builds validated domain objects instead of leaking maps, raw YAML nodes, or strings into domain behavior.
- Add focused provider/adapter tests first, including failure cases, before implementing the adapter or resource changes.

## Implementation Boundaries

- Do not merge `crop-growth.yml` or `crop-yields.yml` into `crop-profiles.yml`.
- Do not change runtime behavior.
- Do not wire the provider in plugin startup.
- Raw YAML must stay in infrastructure.
- The provider port belongs to `crops/performance/domain`; the YAML adapter belongs to `crops/performance/infrastructure`.
- Do not create shared YAML utilities unless real duplication justifies them.

## Test/Verification Expectations

- Follow TDD for provider and adapter behavior.
- Add infrastructure tests for valid profiles.
- Test missing crop profile as unsupported crop-performance condition.
- Test unsupported crop keys.
- Test duplicate YAML keys.
- Test invalid normalized values.
- Test malformed YAML.
- Test I/O failure.
- Test resource packaging includes `crop-profiles.yml`.
- Run focused tests, then `cd java && ./gradlew test build --no-daemon`.
- Run `git diff --check`.

## Dependencies

- Card 3A: Add Crop Environmental State Domain.
- Card 3B: Add Crop Performance Profile Domain.

## Risks Or Migration Notes

- Missing crop profile is not vanilla fallback here. It is converted to neutral performance by `CropPerformanceService` in Card 5B.
- The transitional growth and yield YAML files remain authoritative for their existing behavior until later cards consume crop performance.

## Acceptance Behavior

1. Given a valid `crop-profiles.yml`, when profiles are loaded, then supported `CropKind` profiles become typed domain profile objects.
2. Given a missing crop profile, when the provider is queried, then the application layer can distinguish the unsupported crop-performance condition from real failures.
3. Given malformed config, invalid normalized values, duplicate keys, I/O failures, or programming errors, when loading occurs, then those failures propagate.

## Stop Conditions

Stop and report if the adapter work requires changing crop growth behavior, crop yield behavior, runtime composition, or the existing transitional config files.
