---
id: DB-CROPS-005
type: runtime-composition
bounded_context: crops
capability: growth
layer: pluginruntime
status: Ready
expected_commit: "feat(crops): wire wheat growth runtime"
---

# Goal Card: Wire wheat growth runtime

## Status

Ready

## Goal

Wire the existing wheat growth policy provider, application service, and Paper listener into plugin startup so configured natural wheat growth behavior is active at runtime.

## Why Now

The crops/growth domain policy, YAML-backed provider, application service, and isolated Paper `BlockGrowEvent` listener now exist and have passed focused verification. The next smallest implementation slice is composition only: instantiate the existing collaborators, ensure the default crop-growth resource is available, and register the already-tested listener.

Do not justify work only because a future target architecture mentions it.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/0003-wheat-growth-chance-policy.goal.md`
- `docs/0004-yaml-wheat-growth-policy-provider.goal.md`
- `docs/0005-biome-aware-wheat-growth-service.goal.md`
- `docs/0006-paper-wheat-growth-listener.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChanceVariationSource.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/PaperWheatGrowthListener.java`
- Existing runtime wiring for biome, seasons, ore drops, and resource-backed YAML providers.
- Existing plugin lifecycle tests, MockBukkit tests, and JAR/resource checks.

## Current State

- `crops/growth/domain` owns wheat growth chance validation, decision logic, variation source port, provider port, and unsupported-policy behavior.
- `crops/growth/infrastructure` owns `YamlWheatGrowthChancePolicyProvider` and `PaperWheatGrowthListener`.
- `crops/growth/application/WheatGrowthService` resolves the biome, loads the configured wheat policy, delegates to the domain policy, and preserves vanilla behavior for explicit unsupported biome or policy cases.
- `crop-growth.yml` exists as a packaged resource from the provider slice.
- The wheat growth listener exists but is not registered in `pluginruntime`, so crop growth behavior is not active at runtime.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `pluginruntime`

Rules:

- This card is composition/startup only.
- `pluginruntime` may import crops, biome, seasons, ore, and infrastructure classes only to wire runtime dependencies.
- No package outside `pluginruntime` may import `pluginruntime`.
- Do not move decision logic into `DynamicBiomes`.
- Do not duplicate YAML parsing, biome resolution, or growth chance logic in `DynamicBiomes`.
- Domain and application packages must remain free of Bukkit/Paper, YAML, file I/O, and pluginruntime imports.
- Infrastructure packages must not become responsible for plugin lifecycle orchestration.

## Acceptance Behavior

1. Given the plugin starts with no existing `crop-growth.yml` in the plugin data folder, when startup reaches resource initialization, then the packaged default `crop-growth.yml` is made available using the existing project resource pattern.
2. Given plugin startup succeeds, then `YamlWheatGrowthChancePolicyProvider` is constructed for the runtime `crop-growth.yml` location using the existing project convention for YAML-backed providers.
3. Given plugin startup succeeds, then `WheatGrowthService` is constructed with the runtime `BiomeResolver` and wheat growth policy provider.
4. Given plugin startup succeeds, then `PaperWheatGrowthListener` is registered with Bukkit/Paper using the existing listener registration pattern.
5. Given the listener receives a wheat `BlockGrowEvent` after runtime registration, then the already-implemented listener/service/provider path is reachable from the plugin runtime.
6. Given malformed or invalid `crop-growth.yml`, then startup does not silently preserve vanilla behavior by swallowing parser/configuration failures; follow the existing project convention for invalid YAML-backed startup configuration.
7. Given unsupported biome or unsupported wheat policy at event time, then behavior remains handled by `WheatGrowthService` as vanilla-preserving `ALLOW_GROWTH`; do not add duplicate fallback logic in pluginruntime.

## TDD Plan

1. Add or update the smallest runtime composition test first if the current test setup already supports plugin lifecycle or registration testing safely.
2. Prefer existing runtime tests and MockBukkit patterns. Do not introduce broad new test infrastructure just for this slice.
3. If listener registration cannot be tested safely, add the narrowest possible test around any extractable composition seam only if such a seam already exists; otherwise report the test limitation and rely on full build plus resource/JAR verification.
4. Confirm the focused test fails for the expected reason when practical.
5. Implement the smallest startup wiring change.
6. Run focused tests, if any.
7. Run full verification.
8. Run the JAR/resource check and confirm `crop-growth.yml` is packaged.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`

Tests, only if safe and consistent with existing project style:

- Existing pluginruntime lifecycle test file, if one exists.
- A new pluginruntime lifecycle test only if the repository already has a safe pattern for plugin enable/registration tests.

Documentation, only if required by `AGENTS.md` because runtime behavior documentation changes:

- `ARCHITECTURE.md`
- `AGENTS.md`

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: `DynamicBiomes`

Responsibility:

- Ensure `crop-growth.yml` is available at runtime using the existing resource initialization pattern.
- Compose the runtime crops/growth provider, service, variation source, and listener.
- Register `PaperWheatGrowthListener` during plugin startup.
- Keep lifecycle code limited to startup/composition.

Collaborators:

- `YamlWheatGrowthChancePolicyProvider`
- `WheatGrowthService`
- `PaperWheatGrowthListener`
- `WheatGrowthChanceVariationSource`
- Runtime `BiomeResolver`
- Bukkit/Paper plugin manager

Must not:

- Handle `BlockGrowEvent` directly.
- Parse `crop-growth.yml` directly if the YAML provider already owns parsing.
- Implement wheat chance decision logic.
- Resolve biomes outside the existing service path.
- Add commands, action bars, sounds, logs, public API, or diagnostics.
- Wire broad crop kinds beyond wheat.

## Out of Scope

Explicitly forbidden work:

- Changes to wheat growth domain decisions.
- Changes to YAML schema beyond using the existing `crop-growth.yml` resource.
- Changes to `PaperWheatGrowthListener` behavior unless a compile-time constructor/API mismatch makes a minimal adjustment necessary.
- New crop kinds.
- Bonemeal/player-triggered special handling.
- Growth acceleration or scheduled crop ticks.
- Season effects.
- Inspect commands or player-facing diagnostics.
- Public API.
- Database persistence.
- Configuration reload commands.
- No-edit audit.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- `PaperWheatGrowthListener`, `WheatGrowthService`, or `YamlWheatGrowthChancePolicyProvider` does not exist;
- `crop-growth.yml` does not exist as a packaged resource and adding it would require changing the YAML schema;
- runtime wiring requires broad refactoring of `DynamicBiomes` beyond local composition/startup changes;
- no suitable runtime `BiomeResolver` is available to pass into `WheatGrowthService`;
- no production `WheatGrowthChanceVariationSource` can be supplied without adding non-composition feature logic; if the port is a functional interface, a minimal runtime lambda using the JDK random source is acceptable;
- implementing this card would require domain or application packages to import Bukkit/Paper, YAML, file I/O, or pluginruntime;
- the implementation would create unused scaffolding;
- enabling runtime behavior requires listener behavior changes not already covered by the listener tests.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java
git diff -- java/app/src/main/resources
git diff -- java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime || true
git grep -n "PaperWheatGrowthListener\|YamlWheatGrowthChancePolicyProvider\|WheatGrowthService\|crop-growth.yml" -- java/app/src/main/java java/app/src/main/resources java/app/src/test/java
```

Architecture checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application || true
git grep -n "pluginruntime" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes | grep -v "/pluginruntime/" || true
```

JAR/resource check:

```bash
cd java && ./gradlew build --no-daemon
jar tf app/build/libs/*.jar | sort | grep -E "plugin.yml|crop-growth.yml|ore-drops.yml|biome-profiles.yml|season-profiles.yml|season-cycle.yml|DynamicBiomes"
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated, or exact reason no focused runtime test was safe.
3. Commands run and results.
4. Whether `crop-growth.yml` is packaged and initialized using the project resource pattern.
5. Whether `PaperWheatGrowthListener` is registered at runtime.
6. Whether domain/application boundaries stayed clean.
7. Architecture boundary risks.
8. Deferred work.
