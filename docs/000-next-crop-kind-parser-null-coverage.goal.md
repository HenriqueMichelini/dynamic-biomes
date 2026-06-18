---

id: CROP-KIND-PARSER-NULL-COVERAGE
type: domain
bounded_context: crops
capability: growth
layer: domain
status: Ready
expected_commit: "test(crops): cover null crop policy key parsing"
------------------------------------------------------------------

# Goal Card: Cover Null Crop Policy Key Parsing

## Status

Ready

## Goal

Add explicit regression coverage for `CropKind.fromPolicyKey(null)` returning `Optional.empty()` and ensure the existing `CropKindTest.java` is included in the final worktree diff.

## Why Now

The completed crop-growth generalization passed reverification with no HIGH or MEDIUM findings, but the audit found two LOW notes:

* `CropKindTest.java` is untracked and must be included in the eventual commit.
* `CropKind.fromPolicyKey(null)` currently returns `Optional.empty()` by implementation behavior, but that behavior is not explicitly tested.

This is the smallest safe cleanup slice before starting another gameplay feature.

## Read First

* `AGENTS.md`
* `ARCHITECTURE.md`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKind.java`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKindTest.java`

## Current State

* Crop growth generalization supports `wheat`, `carrots`, `potatoes`, and `beetroot`.
* `CropKind.fromPolicyKey(...)` validates configured crop policy keys.
* Existing parser tests cover canonical keys, unsupported keys, padded keys, and blank keys.
* Null input behavior is implemented but not covered by a dedicated test.
* `CropKindTest.java` is currently untracked according to the audit report.

## Architectural Boundary

* Bounded context: `crops`
* Capability: `growth`
* Layer: `domain`

Rules:

* This is a domain test hardening slice.
* Domain tests must use pure Java/domain objects only.
* Do not import Bukkit, Paper, YAML, file I/O, database, or plugin runtime types.
* Do not change crop runtime behavior.
* Do not add new crop kinds.
* Do not touch infrastructure, application, presentation, resources, or runtime wiring unless the repository state proves the audit report is stale.

## Acceptance Behavior

1. Given `null`, when `CropKind.fromPolicyKey(null)` is called, then it returns `Optional.empty()`.

2. Given existing valid crop keys, when parser tests run, then `wheat`, `carrots`, `potatoes`, and `beetroot` still parse successfully.

3. Given invalid crop keys, when parser tests run, then unsupported, blank, and padded keys still return `Optional.empty()`.

4. Given the final worktree is inspected, then `CropKindTest.java` appears in the diff or tracked file list and is not accidentally omitted.

## TDD Plan

1. Add the smallest explicit null-input test to `CropKindTest`.
2. Run the focused domain test.
3. Do not change production code if the test already passes.
4. If the test fails, make the smallest production change in `CropKind.fromPolicyKey(...)` to return `Optional.empty()` for null input.
5. Run full verification.

## Expected Files

Production:

* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKind.java`

Tests:

* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/CropKindTest.java`

Expected production change: none, unless the null behavior is not actually implemented in the current repository.

## Responsibility / Collaboration Notes

Class: `CropKind`

Responsibility:

* Represent supported crop policy identifiers.
* Parse configured policy keys into supported crop identifiers.
* Reject null, blank, padded, and unsupported keys safely.

Must not:

* Import Bukkit/Paper `Material`.
* Parse YAML directly.
* Know about listener behavior.
* Know about current season, biome resolution, or crop growth event handling.

## Out of Scope

Explicitly forbidden work:

* Do not add new crop kinds.
* Do not change crop growth chance behavior.
* Do not edit `crop-growth.yml`.
* Do not edit `PaperCropMaterialMapper`.
* Do not edit `YamlCropGrowthPolicyProvider`.
* Do not edit `PaperCropGrowthListener`.
* Do not edit diagnostics.
* Do not update `ARCHITECTURE.md` unless the current docs are now inaccurate.
* Do not start tree, animal, ecological-region, database, reload, or command work.

## Stop Conditions

Stop and report instead of editing if:

* `CropKind.java` does not exist.
* `CropKindTest.java` does not exist and creating it would duplicate an existing test under a different package/name.
* The current parser intentionally throws on null and this behavior is documented.
* The task conflicts with `ARCHITECTURE.md`.
* The task conflicts with `AGENTS.md`.
* The working tree contains unrelated changes that make this cleanup unsafe.
* Adding the test would require introducing new test dependencies.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*CropKindTest" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Run domain import scan:

```bash
rg -n "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" \
  java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain \
  java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain
```

If using `rtk`, preserve the project’s established wrapper style:

```bash
rtk git diff --check
cd java && rtk ./gradlew test --tests "*CropKindTest" --no-daemon
cd java && rtk ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Whether production code changed.
3. Tests added or updated.
4. Commands run and exact results.
5. Whether `CropKindTest.java` is still untracked or now accounted for.
6. Architecture boundary risks.
7. Suggested commit message.
