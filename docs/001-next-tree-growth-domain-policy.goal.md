---

id: TREE-GROWTH-DOMAIN-POLICY
type: domain
bounded_context: trees
capability: growth
layer: domain
status: Draft
expected_commit: "feat(trees): model tree growth policy decision"
-----------------------------------------------------------------

# Goal Card: Model Tree Growth Policy Decision

## Status

Draft

This card becomes Ready only after the implementation agent inspects the repository and confirms that adding a new `trees/growth/domain` package is justified by the current project direction and does not conflict with `ARCHITECTURE.md`.

## Goal

Create the pure domain model for tree growth allow/cancel decisions using configured chance and optional season-owned tree growth factors, without Bukkit, YAML, listener, runtime, or persistence wiring.

## Why Now

Crop growth is already implemented and generalized for simple ageable crops. The documented deferred work includes broader season effects on crops, trees, and animals. The smallest safe next step toward tree behavior is a pure domain policy with deterministic tests, not runtime event handling.

This slice establishes tree growth decision rules only. It must not create unused scaffolding or runtime behavior.

## Read First

* `AGENTS.md`
* `ARCHITECTURE.md`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/seasons/identity/domain/SeasonId.java`

## Current State

Expected current state:

* Crop growth has a tested domain policy model for configured allow/cancel decisions.
* Crop growth supports configured crop identifiers such as `wheat`, `carrots`, `potatoes`, and `beetroot`.
* The tree growth bounded context is not yet implemented or wired at runtime.
* `seasons` publishes `SeasonId`, and downstream feature domains may consume published season identity.

## Architectural Boundary

* Bounded context: `trees`
* Capability: `growth`
* Layer: `domain`

Rules:

* Domain code must be pure Java.
* Domain may import published upstream vocabulary only when listed in `ARCHITECTURE.md`, such as `SeasonId`.
* Domain must not import Bukkit, Paper, YAML, file I/O, database, plugin runtime, or infrastructure packages.
* Do not add application, infrastructure, presentation, YAML resources, listener registration, or runtime wiring in this slice.
* Do not create empty packages.
* Do not add placeholder ports unless this slice directly uses them.
* Do not modify crop behavior.

## Acceptance Behavior

1. Given a tree growth policy with base allow chance `1.0`, when a growth decision is evaluated with any variation roll from `0.0` inclusive to `1.0` exclusive, then the decision is allow.

2. Given a tree growth policy with base allow chance `0.0`, when a growth decision is evaluated, then the decision is cancel.

3. Given a tree growth policy with base allow chance `0.5`, when the variation roll is below `0.5`, then the decision is allow.

4. Given a tree growth policy with base allow chance `0.5`, when the variation roll is equal to or above `0.5`, then the decision is cancel.

5. Given a current season with a configured seasonal factor, when a growth decision is evaluated, then the effective chance is `baseChance * seasonalFactor`.

6. Given a current season without a configured seasonal factor, when a growth decision is evaluated, then the seasonal factor defaults to `1.0`.

7. Given an effective chance above `1.0`, then it is capped at `1.0`.

8. Given an effective chance below `0.0`, then construction or calculation rejects the invalid value rather than silently producing undefined behavior.

9. Given invalid base chance or seasonal factor values, then construction fails with a clear exception.

## TDD Plan

1. Create the smallest domain test class first under:

   * `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthPolicyTest.java`
2. Add tests for allow/cancel thresholds without seasons.
3. Add tests for seasonal factor behavior using published `SeasonId`.
4. Add tests for invalid chance/factor values.
5. Confirm tests fail because production classes do not exist.
6. Implement the smallest production domain classes required.
7. Run focused domain tests.
8. Run full verification.

## Expected Files

Production:

* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthPolicy.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthChance.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthSeasonalFactor.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthDecision.java`
* `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthChanceVariationSource.java`

Tests:

* `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain/TreeGrowthPolicyTest.java`
* Additional focused domain tests only if the production classes become too large for one readable test class.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `TreeGrowthPolicy`

Responsibility:

* Hold a configured base tree growth allow chance.
* Hold optional seasonal factors keyed by published `SeasonId`.
* Produce allow/cancel decisions from deterministic variation values.
* Calculate effective chance from base chance and seasonal factor.

Collaborators:

* `TreeGrowthChance`
* `TreeGrowthSeasonalFactor`
* `TreeGrowthChanceVariationSource`
* `SeasonId`

Must not:

* Resolve biome.
* Query current season.
* Parse YAML.
* Listen to Bukkit events.
* Mutate world state.

Class: `TreeGrowthChance`

Responsibility:

* Represent a validated probability in the closed range `0.0` to `1.0`.

Must not:

* Know about seasons, trees, Bukkit, YAML, or configuration files.

Class: `TreeGrowthSeasonalFactor`

Responsibility:

* Represent a validated non-negative multiplier used to adjust tree growth chance.

Must not:

* Know about current season lookup or profile loading.

Class: `TreeGrowthDecision`

Responsibility:

* Represent whether a natural tree growth attempt should be allowed or cancelled.

Must not:

* Contain Bukkit event references.

Class: `TreeGrowthChanceVariationSource`

Responsibility:

* Provide deterministic-testable variation values.

Must not:

* Use Bukkit randomness.
* Depend on runtime services.

## Out of Scope

Explicitly forbidden work:

* Do not add Bukkit/Paper tree listeners.
* Do not add YAML tree-growth configuration.
* Do not add `TreeKind` unless the policy cannot be meaningful without it.
* Do not add biome-aware tree services.
* Do not add `TreeGrowthPolicyProvider`.
* Do not add pluginruntime wiring.
* Do not add commands or diagnostics.
* Do not update crop, ore, biome, or season runtime behavior.
* Do not implement animal behavior.
* Do not implement ecological region state.
* Do not create placeholder application/infrastructure/presentation packages.
* Do not update `ARCHITECTURE.md` unless the implementation intentionally changes documented status.

## Stop Conditions

Stop and report instead of editing if:

* `ARCHITECTURE.md` forbids a `trees/growth` bounded context or capability.
* The repository already contains a tree growth domain with conflicting names or behavior.
* Adding the package would create unused scaffolding rather than tested domain behavior.
* The implementation requires Bukkit, YAML, runtime wiring, or persistence.
* The implementation requires new dependencies.
* The working tree contains uncommitted crop-generalization changes that make the diff unsafe.
* Tests cannot be written without framework mocks or infrastructure dependencies.

## Verification

Run:

```bash id="spxtv1"
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test --tests "*TreeGrowthPolicyTest" --no-daemon
cd java && ./gradlew test build --no-daemon
```

Run domain import scan:

```bash id="5sr7al"
rg -n "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" \
  java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain \
  java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/trees/growth/domain
```

If using `rtk`, preserve wrapper style:

```bash id="vad82n"
rtk git diff --check
cd java && rtk ./gradlew test --tests "*TreeGrowthPolicyTest" --no-daemon
cd java && rtk ./gradlew test build --no-daemon
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Whether a TDD red phase was observed.
4. Commands run and exact results.
5. Domain purity scan result.
6. Architecture boundary risks.
7. Deferred work.
8. Suggested commit message.
