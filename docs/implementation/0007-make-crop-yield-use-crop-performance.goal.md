---
id: CROP-PERF-0007
type: application
bounded_context: crops
capability: yield
layer: application
status: Ready
expected_commit: "feat(crops): apply crop performance to harvest yield"
---

# Goal Card: Make Crop Yield Use Crop Performance

## Goal

Apply crop performance to existing DynamicBiomes crop yield behavior without double-applying season climate.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0005-add-crop-performance-application-service.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`

## Current-State Problem

Yield currently applies biome-scoped multiplier, legacy crop-yield seasonal factor, and `CropYieldClimateFactorCalculator`. Once performance owns environmental interpretation, the old climate factor would duplicate season influence.

## Target-State Outcome

Yield uses this transitional formula:

```text
adjustedQuantity = probabilisticRound(
    vanillaProduceQuantity
        * selectedBiomeCropMultiplier
        * legacyCropYieldSeasonalFactor
        * cropPerformance.harvestQuantityFactor
)
```

Required domain decisions:

- Yield must not double-apply season climate.
- Once yield consumes `CropPerformanceResult`, do not apply `CropYieldClimateFactorCalculator`.
- `crop-yields.yml` remains transitional for one phase.
- Biome-scoped crop yield multipliers are transitional once crop performance exists.
- Missing crop profile / unsupported crop-performance profile becomes neutral `CropPerformanceResult`.
- Unsupported biome remains an unsupported upstream/environment condition so yield consumers preserve vanilla behavior.
- No quality tier, quality probability, quality roll, item quality, quality diagnostics, or `/dynamicbiomes inspect` quality output.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- Possibly `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/yield/application`
- Existing crop yield infrastructure tests if constructor wiring changes require updates.

## OOP / DDD / TDD Guardrails

- Keep yield quantity policy and rounding behavior in `crops/yield/domain`; keep crop-performance environmental interpretation in `crops/performance`.
- Use the yield application service to orchestrate the transitional formula, not to duplicate crop-performance scoring or YAML parsing.
- Pluginruntime changes are allowed only for constructor/composition wiring required by this card.
- Do not import upstream infrastructure or Bukkit/Paper events into application or domain packages.
- Add focused yield application tests first for neutral performance, non-neutral harvest factor, unsupported fallbacks, and climate-factor removal before production changes.

## Implementation Boundaries

- Do not change `crop-yields.yml`.
- Do not remove biome-scoped yield policy in this card.
- Do not apply `CropYieldClimateFactorCalculator` once `CropPerformanceResult` is wired into yield.
- Do not add quality output.
- Preserve vanilla yield fallback for unsupported biome or unsupported crop yield policy.
- Do not change listener semantics.

## Test/Verification Expectations

- Follow TDD in the smallest affected yield service tests.
- Test neutral performance preserving current DynamicBiomes yield behavior except removal of duplicate climate factor.
- Test `harvestQuantityFactor` changing quantity.
- Test missing crop performance staying neutral.
- Test unsupported biome or unsupported crop yield policy preserving vanilla harvest quantity.
- Test no use of yield-owned climate factor once crop performance is wired.
- Run focused yield tests, then `cd java && ./gradlew test build --no-daemon`.
- Run `git diff --check`.

## Dependencies

- Card 5: Add Crop Performance Application Service.

## Risks Or Migration Notes

- `crop-yields.yml` seasonal factors remain as legacy crop-yield tuning for one transitional phase and should be reviewed for removal or replacement later.
- This card intentionally removes the active climate factor from yield composition once crop performance supplies environmental interpretation.

## Acceptance Behavior

1. Given neutral crop performance, when yield is evaluated, then current DynamicBiomes yield behavior is preserved except that season climate is not double-applied.
2. Given non-neutral crop performance, when yield is evaluated, then the transitional formula uses `cropPerformance.harvestQuantityFactor`.
3. Given unsupported biome or unsupported crop yield policy, when yield is evaluated, then vanilla harvest quantity is preserved.
4. Given crop performance is wired, when code is inspected, then `CropYieldClimateFactorCalculator` is not applied in active yield formula.

## Stop Conditions

Stop and report if the change requires deleting biome-scoped yield policy, changing `crop-yields.yml`, adding quality output, or combining cleanup from Card 8 into this card.
