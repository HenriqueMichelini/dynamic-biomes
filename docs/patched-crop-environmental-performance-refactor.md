## Architecture Documentation Notes

Patch `ARCHITECTURE.md` to state these rules explicitly:

- `crops/performance` owns crop-specific interpretation. Biome, seasons, and future weather remain upstream environmental contributors and must not gain crop vocabulary.
- `CropEnvironmentalState` is a crop-side read model with normalized `0.0..1.0` variables: wind speed, rain strength, humidity, temperature, solar incidence, and soil fertility.
- V1 composition:
  - Humidity and temperature come from `BiomeProfile` and may be modified by season factors multiplicatively.
  - Soil fertility comes only from `BiomeProfile.Fertility`.
  - Wind speed, rain strength, and solar incidence default neutral until upstream contracts exist.
- Season-modified variables use `finalVariable = clamp01(biomeVariable * seasonVariableFactor)`.
- Do not apply season or weather factors to soil fertility in v1.
- Missing crop profile is represented as an unsupported crop-performance condition at the provider/application boundary. `CropPerformanceService` converts it into a neutral `CropPerformanceResult`.
- Malformed config, invalid normalized values, duplicate keys, I/O failures, and programming errors still propagate.
- `CropPerformanceResult` exposes only optional `overallScore`, `growthSpeedFactor`, `growthChanceFactor`, and `harvestQuantityFactor`.
- No quality tier, quality probability, quality roll, item quality, quality diagnostics, or `/dynamicbiomes inspect` quality output.
- `crop-growth.yml` and `crop-yields.yml` remain transitional. `crop-profiles.yml` becomes the crop-owned preference/profile source.
- Biome-scoped crop yield multipliers are transitional once crop performance exists.
- Yield must not double-apply season climate. Once yield consumes `CropPerformanceResult`, remove the old yield-owned season-climate factor from the active formula.
- Deferred: weather context, variable interactions, dynamic biome-region variation, active season/weather generated variation, destructive crop events, local soil persistence, fertilizer, irrigation, farmland degradation, block-level fertility, and runtime growth-speed wiring.

## Patched Goal Cards

### 1. Reconcile Crop Environmental Design In Architecture

**Goal:** Update architecture documentation to define `crops/performance` as the crop-owned environmental interpretation capability.

**Current-State Problem:** `ARCHITECTURE.md` documents growth and yield behavior but presents biome-scoped crop yield policy as the crop environmental target.

**Target-State Outcome:** Architecture clearly separates upstream environmental contributors from crop-owned environmental state, crop profiles, and performance factors.

**Files Or Areas Likely Affected:** `ARCHITECTURE.md`.

**Implementation Boundaries:** Docs only. No production code, tests, YAML resources, providers, services, listeners, or runtime wiring.

**Test/Verification Expectations:** Run `git diff --check`. Stop if production/test code changes.

**Dependencies:** None.

**Risks Or Migration Notes:** Must explicitly exclude quality and prevent crop-specific vocabulary from entering biome or seasons.

### 2. Define Crop-Owned CropEnvironmentalState Composition

**Goal:** Specify v1 environmental composition for crop performance.

**Current-State Problem:** There is no crop-side read model for environmental variables, and season handling could be misread as modifying all variables.

**Target-State Outcome:** V1 composition is unambiguous: humidity and temperature come from `BiomeProfile` and may be season-modified; soil fertility comes only from `BiomeProfile.Fertility`; wind speed, rain strength, and solar incidence default neutral.

**Files Or Areas Likely Affected:** `ARCHITECTURE.md`; later implementation under `crops/performance`.

**Implementation Boundaries:** Do not introduce weather, local soil persistence, fertilizer, irrigation, farmland degradation, block-level fertility, or variable interactions. Do not apply season or weather to soil fertility in v1.

**Test/Verification Expectations:** Later implementation tests should cover `finalVariable = clamp01(biomeVariable * seasonVariableFactor)` for humidity/temperature only, neutral defaults, and biome-only soil fertility.

**Dependencies:** Card 1.

**Risks Or Migration Notes:** This card prevents accidental soil fertility double-counting or hidden local-soil modeling.

### 3. Add Pure Crop Performance Domain Model

**Goal:** Implement pure crop performance domain types and scoring.

**Current-State Problem:** Growth and yield own separate factors, and no crop model compares current environmental state to crop preferences.

**Target-State Outcome:** `crops/performance/domain` calculates `CropPerformanceResult` from a crop profile and `CropEnvironmentalState`.

**Files Or Areas Likely Affected:** `crops/performance/domain` and matching tests.

**Implementation Boundaries:** Domain only. No Bukkit, YAML, file I/O, services, providers, runtime wiring, or quality concepts.

**Test/Verification Expectations:** Pure tests for normalized value validation, exact preference match, poor match, derived factors, optional overall score, and no quality fields.

**Dependencies:** Cards 1 and 2.

**Risks Or Migration Notes:** `growthSpeedFactor` exists immediately even though runtime growth-speed wiring remains deferred.

### 4. Add Crop Performance Profile Provider And YAML Adapter

**Goal:** Introduce crop-owned preference profiles loaded from `crop-profiles.yml`.

**Current-State Problem:** Existing crop configs describe growth chances and yield multipliers, not crop preferred environmental conditions.

**Target-State Outcome:** A typed provider port and YAML adapter load crop preference profiles for supported `CropKind` values.

**Files Or Areas Likely Affected:** `crops/performance/domain`, `crops/performance/infrastructure`, `crop-profiles.yml`, resource tests.

**Implementation Boundaries:** Do not merge `crop-growth.yml` or `crop-yields.yml` into `crop-profiles.yml`. Do not change runtime behavior.

**Test/Verification Expectations:** Infrastructure tests for valid profiles, missing crop profile as unsupported crop-performance condition, unsupported crop keys, duplicate YAML keys, invalid normalized values, malformed YAML, I/O failure, and resource packaging.

**Dependencies:** Card 3.

**Risks Or Migration Notes:** Missing crop profile is not vanilla fallback here. It is converted to neutral performance by `CropPerformanceService`.

### 5. Add Crop Performance Application Service

**Goal:** Create orchestration that resolves environment, loads crop profile, and returns crop performance.

**Current-State Problem:** Growth and yield services independently resolve biome/season and have no shared crop performance result.

**Target-State Outcome:** `CropPerformanceService` composes v1 `CropEnvironmentalState` from published upstream contracts and delegates performance calculation to domain.

`CropPerformanceService` converts missing crop profile / unsupported crop-performance profile into neutral `CropPerformanceResult`.

`CropPerformanceService` must not convert `UnsupportedBiomeException` into neutral performance. Unsupported biome remains an unsupported upstream/environment condition so growth/yield consumers preserve vanilla behavior according to existing fallback semantics.

**Files Or Areas Likely Affected:** `crops/performance/application` and matching tests.

**Implementation Boundaries:** Application orchestration only. No Bukkit listeners, YAML parsing, command output, or runtime registration.

**Test/Verification Expectations:** Application tests with stubs for biome resolver, current season query, season profile provider, and profile provider. Cover multiplicative humidity/temperature season factors, biome-only soil fertility, neutral unavailable variables, missing crop profile converted to neutral result, unsupported biome propagation / unsupported-environment result separately from missing crop profile neutralization, and real failure propagation.

**Dependencies:** Cards 2, 3, and 4.

**Risks Or Migration Notes:** Missing crop profile / unsupported crop-performance profile preserves existing DynamicBiomes behavior through neutral factors, not vanilla fallback. `UnsupportedBiomeException` is not neutralized here; it remains an upstream/environment unsupported condition so growth/yield fallback semantics can preserve vanilla behavior.

### 6. Make Crop Growth Use Crop Performance

**Goal:** Apply crop performance to existing DynamicBiomes crop growth behavior.

**Current-State Problem:** Growth uses configured biome/crop chance and legacy crop growth seasonal factor only.

**Target-State Outcome:** Existing growth chance remains active and is multiplied by `cropPerformance.growthChanceFactor`; `growthSpeedFactor` remains available but not wired to runtime speed behavior.

**Files Or Areas Likely Affected:** `crops/growth/application`, possibly `crops/growth/domain`, pluginruntime composition, growth tests.

**Implementation Boundaries:** Do not change `crop-growth.yml`. Do not change listener semantics. Do not implement growth-speed runtime wiring.

**Test/Verification Expectations:** Tests for neutral performance preserving current DynamicBiomes growth behavior, non-neutral growth factor affecting decisions, missing crop performance preserving current DynamicBiomes behavior, and unsupported biome/policy preserving vanilla growth.

**Dependencies:** Card 5.

**Risks Or Migration Notes:** This is the first explicit crop growth semantic change.

### 7. Make Crop Yield Use Crop Performance

**Goal:** Apply crop performance to existing DynamicBiomes crop yield behavior without double-applying season climate.

**Current-State Problem:** Yield currently applies biome-scoped multiplier, legacy crop-yield seasonal factor, and `CropYieldClimateFactorCalculator`. Once performance owns environmental interpretation, the old climate factor would duplicate season influence.

**Target-State Outcome:** Yield uses this transitional formula:

```text
adjustedQuantity = probabilisticRound(
    vanillaProduceQuantity
        * selectedBiomeCropMultiplier
        * legacyCropYieldSeasonalFactor
        * cropPerformance.harvestQuantityFactor
)
```

**Files Or Areas Likely Affected:** `crops/yield/application`, `crops/yield/domain`, pluginruntime composition, yield tests.

**Implementation Boundaries:** Do not change `crop-yields.yml`. Do not remove biome-scoped yield policy in this card. Do not apply `CropYieldClimateFactorCalculator` once `CropPerformanceResult` is wired into yield. Do not add quality output.

**Test/Verification Expectations:** Tests for neutral performance preserving current DynamicBiomes yield behavior except removal of duplicate climate factor, harvest factor changing quantity, missing crop performance staying neutral, unsupported biome/policy preserving vanilla, and no use of yield-owned climate factor.

**Dependencies:** Card 5.

**Risks Or Migration Notes:** `crop-yields.yml` seasonal factors remain as legacy crop-yield tuning for one transitional phase and should be reviewed for removal or replacement later.

### 8. Remove Superseded Crop-Yield Environmental Calculators

**Goal:** Remove or retire yield-specific environmental calculators after crop performance owns environmental interpretation.

**Current-State Problem:** `CropYieldClimateFactorCalculator`, `CropYieldBiomeFactorCalculator`, and `CropYieldEnvironmentalFactorCalculator` duplicate or conflict with `crops/performance`.

**Target-State Outcome:** Yield no longer owns environmental interpretation; it consumes crop performance factors.

**Files Or Areas Likely Affected:** `crops/yield/domain`, `crops/yield/application`, yield tests, `ARCHITECTURE.md`.

**Implementation Boundaries:** Cleanup only after Card 7. Do not change formulas beyond removing dead transitional code. Do not delete coverage unless equivalent `crops/performance` tests exist.

**Test/Verification Expectations:** Run `rg` for removed class references, `git diff --check`, and `cd java && ./gradlew test build --no-daemon`.

**Dependencies:** Cards 6 and 7.

**Risks Or Migration Notes:** Stop if any calculator remains wired into active runtime after Card 7.

## Verification

Documentation-only correction verification remains:

```bash
git diff --check
```

Production or test code changes are outside this correction task.
