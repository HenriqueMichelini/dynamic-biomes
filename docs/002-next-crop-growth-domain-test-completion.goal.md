---

id: CROP-GROWTH-DOMAIN-TEST-COMPLETION
type: domain
bounded_context: crops
capability: growth
layer: domain
status: Ready
expected_commit: "test(crops): complete crop growth domain coverage"
--------------------------------------------------------------------

# Goal Card: Complete Crop Growth Domain Test Coverage

## Status

Ready

## Goal

Add focused pure-domain tests for the crop growth domain so all crop growth value objects, policy decisions, parser behavior, seasonal factors, and unsupported-policy behavior are explicitly covered without changing runtime behavior.

## Why Now

Crop growth was generalized from wheat/carrots to wheat/carrots/potatoes/beetroot and passed implementation reverification. A cleanup slice then added explicit null-input coverage for `CropKind.fromPolicyKey(null)`.

Before starting a new bounded context such as `trees/growth`, the existing `crops/growth/domain` should be fully tested. This prevents carrying weak crop-domain confidence into the next feature area.

This task is test-hardening only. It is justified by the current crop domain implementation and the completed crop generalization, not by future tree/animal/ecological-region work.

## Read First

* `AGENTS.md`
* `ARCHITECTURE.md`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/identity/domain/SeasonId.java`

## Current State

Expected current crop domain state:

* `CropKind` supports `wheat`, `carrots`, `potatoes`, and `beetroot`.
* `CropKind.fromPolicyKey(...)` returns `Optional.empty()` for null, blank, padded, unsupported, or otherwise invalid keys.
* `CropGrowthChance` models a configured natural crop growth allow probability.
* `CropGrowthSeasonalFactor` models a crop-owned seasonal multiplier/factor.
* `CropGrowthPolicy` combines crop kind, base chance, optional seasonal factors, and a deterministic variation source to produce an allow/cancel decision.
* `CropGrowthDecision` represents allow/cancel behavior.
* `CropGrowthPolicyProvider` and `UnsupportedCropGrowthPolicyException` are crop-domain ports/contracts.
* Domain code is pure Java and must not depend on Bukkit/Paper/YAML/file I/O/database/runtime.

## Architectural Boundary

* Bounded context: `crops`
* Capability: `growth`
* Layer: `domain`

Rules:

* Domain tests must use pure Java/domain objects only.
* Tests must mirror the production package structure.
* Do not import Bukkit, Paper, YAML, file I/O, database, pluginruntime, or infrastructure packages.
* Do not edit application, infrastructure, presentation, runtime, resources, or docs unless a currently failing domain test proves the domain contract is wrong and docs are inaccurate.
* Do not change crop runtime behavior.
* Do not add new crop kinds.
* Do not add speculative abstractions.

## Acceptance Behavior

Add or confirm explicit test coverage for these behaviors.

### CropKind

1. Given canonical supported policy keys `wheat`, `carrots`, `potatoes`, and `beetroot`, when parsed, then each returns the expected `CropKind`.

2. Given `null`, blank, padded, differently cased, or unsupported policy keys, when parsed, then `Optional.empty()` is returned.

3. Given any supported `CropKind`, when its policy key is read, then the key is stable and matches the YAML-facing lowercase identifier.

### CropGrowthChance

4. Given chance values `0.0`, a midpoint such as `0.5`, and `1.0`, when constructed, then construction succeeds and preserves the exact value.

5. Given values below `0.0`, above `1.0`, `NaN`, positive infinity, or negative infinity, when constructed, then construction fails with a clear exception.

### CropGrowthSeasonalFactor

6. Given valid factor values such as `0.0`, `1.0`, and a multiplier above `1.0`, when constructed, then construction succeeds and preserves the exact value.

7. Given values below `0.0`, `NaN`, positive infinity, or negative infinity, when constructed, then construction fails with a clear exception.

### CropGrowthDecision

8. Given the allow decision factory/value, when inspected, then it reports allow/growth-not-cancelled semantics.

9. Given the cancel decision factory/value, when inspected, then it reports cancel/growth-cancelled semantics.

### CropGrowthPolicy

10. Given base chance `1.0`, when any valid variation roll below `1.0` is used, then the decision is allow.

11. Given base chance `0.0`, when any valid variation roll is used, then the decision is cancel.

12. Given base chance `0.5`, when variation roll is below `0.5`, then the decision is allow.

13. Given base chance `0.5`, when variation roll is equal to `0.5`, then the decision is cancel.

14. Given base chance `0.5`, when variation roll is above `0.5`, then the decision is cancel.

15. Given a current season with configured seasonal factor `0.5` and base chance `0.8`, when evaluated, then effective chance is `0.4`.

16. Given a current season with no configured factor and base chance `0.8`, when evaluated, then effective chance remains `0.8`.

17. Given a seasonal factor pushes effective chance above `1.0`, when evaluated, then effective chance is capped at `1.0`.

18. Given a deterministic variation source returns invalid roll values below `0.0`, equal to `1.0`, above `1.0`, `NaN`, or infinity, when a decision is evaluated, then the policy rejects the invalid roll with a clear exception.

19. Given null required constructor arguments such as crop kind, base chance, seasonal factors map, or variation source, when constructing or evaluating a policy, then the domain fails clearly rather than producing undefined behavior.

### UnsupportedCropGrowthPolicyException / Provider Contract

20. Given unsupported policy exception construction, when inspected, then the message/context is useful enough for application fallback diagnostics.

21. Do not fake infrastructure behavior here. Only test the pure domain contract, not YAML provider behavior.

## TDD Plan

1. Inspect current crop domain tests and list already covered behaviors.
2. Add missing tests class-by-class, using existing naming style.
3. Prefer focused test classes:

   * `CropKindTest`
   * `CropGrowthChanceTest`
   * `CropGrowthSeasonalFactorTest`
   * `CropGrowthDecisionTest`
   * `CropGrowthPolicyTest`
   * `UnsupportedCropGrowthPolicyExceptionTest`, only if the exception has meaningful behavior beyond construction.
4. Confirm new tests fail only if production behavior is missing or unclear.
5. If production behavior already satisfies a new test, keep the test and report that no red phase was observed for that assertion.
6. Make the smallest production change only if a test exposes an actual domain contract gap.
7. Run focused crop domain tests.
8. Run full verification.

## Expected Files

Production:

* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKind.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthChance.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthSeasonalFactor.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthDecision.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthPolicy.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthChanceVariationSource.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/UnsupportedCropGrowthPolicyException.java`

Tests:

* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKindTest.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthChanceTest.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthSeasonalFactorTest.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthDecisionTest.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropGrowthPolicyTest.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/UnsupportedCropGrowthPolicyExceptionTest.java`, only if justified by current exception behavior.

Expected production change: none unless tests expose an actual missing invariant or invalid-roll gap.

These are expected files, not permission to create unrelated scaffolding.

## Responsibility / Collaboration Notes

Class: `CropKind`

Responsibility:

* Represent supported crop policy identifiers.
* Parse YAML-facing crop policy keys into supported domain identifiers.
* Reject unsupported or malformed keys.

Must not:

* Import Bukkit/Paper `Material`.
* Parse YAML.
* Know about listeners, biome resolution, or current season lookup.

Class: `CropGrowthChance`

Responsibility:

* Represent a validated probability in the closed range `0.0` to `1.0`.

Must not:

* Know about crop kind, season, YAML, or Bukkit.

Class: `CropGrowthSeasonalFactor`

Responsibility:

* Represent a validated non-negative finite multiplier used by crop-owned seasonal policy.

Must not:

* Query current season.
* Read season profile configuration.
* Depend on infrastructure.

Class: `CropGrowthDecision`

Responsibility:

* Represent whether a natural crop growth attempt should be allowed or cancelled.

Must not:

* Reference Bukkit events.

Class: `CropGrowthPolicy`

Responsibility:

* Combine crop kind, base chance, optional seasonal factor, and deterministic variation roll into a pure allow/cancel decision.
* Cap effective chance at `1.0`.
* Reject invalid variation rolls.

Must not:

* Resolve biome.
* Query current season.
* Parse YAML.
* Listen to events.
* Mutate world state.

## Out of Scope

Explicitly forbidden work:

* Do not add new crop kinds.
* Do not change supported crop material mapping.
* Do not edit `PaperCropMaterialMapper`.
* Do not edit `PaperCropGrowthListener`.
* Do not edit `YamlCropGrowthPolicyProvider`.
* Do not edit `CropGrowthService`.
* Do not edit `CropGrowthInspectDiagnostic`.
* Do not edit `crop-growth.yml`.
* Do not add tree, animal, ecological-region, database, reload, command, or runtime work.
* Do not add application/infrastructure/presentation packages.
* Do not introduce new dependencies.
* Do not weaken or delete existing tests.

## Stop Conditions

Stop and report instead of editing if:

* the task conflicts with `ARCHITECTURE.md`;
* the task conflicts with `AGENTS.md`;
* crop domain production files do not match the documented current state;
* existing crop domain tests already fully cover every acceptance behavior above;
* adding tests requires Bukkit/Paper/YAML/file I/O/database/runtime dependencies;
* the implementation requires broader architecture changes;
* the working tree contains unrelated changes that make test-hardening unsafe;
* a production change would alter runtime behavior outside the pure domain contract.

## Verification

Run:

```bash id="az673s"
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.*" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Run domain import scan:

```bash id="evu5g2"
rg -n "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" \
  java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain \
  java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain
```

If using `rtk`, preserve wrapper style:

```bash id="zglari"
rtk git diff --check
cd java && rtk ./gradlew test --tests "io.github.henriquemichelini.dynamicbiomes.crops.growth.domain.*" --no-daemon
cd java && rtk ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Whether production code changed.
4. Which acceptance behaviors were already covered before this slice.
5. Which acceptance behaviors were newly covered.
6. Whether any red phase was observed.
7. Commands run and exact results.
8. Domain purity scan result.
9. Architecture boundary risks.
10. Deferred work.
11. Suggested commit message.
