---
id: DB-CROPS-005
type: presentation
bounded_context: crops
capability: growth
layer: presentation
status: Ready
expected_commit: "feat(crops): inspect wheat growth policy"
---

# Goal Card: Inspect wheat growth policy diagnostics

## Status

Ready

## Goal

Extend the read-only `/dynamicbiomes inspect` behavior so inspecting wheat reports the configured DynamicBiomes wheat growth policy diagnostics for the target block.

## Why Now

The wheat growth domain policy, YAML-backed provider, application service, Paper listener, and runtime wiring are implemented and manually validated. The behavior is probabilistic and event-driven, so read-only diagnostics are now useful before broadening crop support.

Do not justify broader crop work only because future crop support is planned.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/presentation/OreInspectCommandExecutor.java`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/presentation/OreInspectCommandExecutorTest.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/WheatGrowthChancePolicyProvider.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/UnsupportedWheatGrowthPolicyException.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/YamlWheatGrowthChancePolicyProvider.java`

## Current State

- `/dynamicbiomes inspect` already reports read-only target block and ore diagnostics.
- Wheat natural growth runtime behavior is active through `PaperWheatGrowthListener` and `WheatGrowthService`.
- `crop-growth.yml` is loaded through `YamlWheatGrowthChancePolicyProvider`.
- Unsupported biome and unsupported wheat policy preserve vanilla growth in the application service.
- There is no crop growth inspect diagnostic yet.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: `presentation`

Rules:

- Crop growth diagnostics must not be implemented inside `biome` or `seasons`.
- Do not make `ore/drops` depend on `crops/growth` domain/application/infrastructure.
- If the current inspect command is hard-owned by `ore/drops/presentation`, preserve boundaries by introducing the smallest neutral command composition or crop-owned diagnostic collaborator needed.
- `pluginruntime` may be touched only for command composition/wiring if required.
- Do not add Bukkit/Paper, YAML, file I/O, or pluginruntime imports to crop domain/application code.
- Do not roll randomness or call the growth decision path for diagnostics.

## Acceptance Behavior

1. Given a player targets a wheat block in a supported biome with a configured wheat growth policy, when `/dynamicbiomes inspect` is run, then the output reports the target material and wheat growth policy support.
2. Given a configured wheat growth chance, when wheat is inspected, then the output reports the configured chance without rolling or simulating a growth decision.
3. Given configured chance `1.0`, when wheat is inspected, then the output reports that DynamicBiomes will not cancel natural wheat growth for that policy.
4. Given configured chance below `1.0`, when wheat is inspected, then the output reports that DynamicBiomes may cancel natural wheat growth for that policy.
5. Given an unsupported biome, when wheat is inspected, then the output reports wheat growth policy as unsupported or unavailable and reports natural growth fallback as vanilla-preserving.
6. Given a supported biome with no configured wheat growth policy, when wheat is inspected, then the output reports wheat growth policy as unsupported or unavailable and reports natural growth fallback as vanilla-preserving.
7. Given a non-wheat target block, when `/dynamicbiomes inspect` is run, then existing ore inspect behavior remains unchanged.
8. Inspect diagnostics must not mutate crop, ore, biome, season, or persistence state.

Suggested user-facing lines may be adjusted to match existing command style, but the information must be explicit:

```text
Wheat growth policy: supported
Configured wheat growth chance: 0.75
May cancel natural growth: yes
```

For unsupported/fallback cases, use explicit wording such as:

```text
Wheat growth policy: unsupported
May cancel natural growth: no (vanilla fallback)
```

## TDD Plan

1. Add or update the smallest relevant command/presentation test first.
2. Cover supported wheat policy output for a deterministic configured chance.
3. Cover unsupported biome and unsupported wheat policy diagnostics.
4. Cover that non-wheat targets preserve existing ore inspect behavior.
5. Confirm the focused test fails for the expected reason when practical.
6. Implement the smallest presentation/composition change.
7. Run focused tests.
8. Run full verification.

## Expected Files

Production:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/<crop-growth-inspect-diagnostic-class>.java`
- Existing inspect command/wiring files only if required to expose the diagnostics through `/dynamicbiomes inspect`.
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java` only if command composition needs one additional crop diagnostic collaborator.

Tests:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/<crop-growth-inspect-diagnostic-class>Test.java`
- Existing inspect command tests only if command-level output must be updated.
- Plugin/runtime command wiring tests only if `DynamicBiomes.java` changes.

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Class: crop growth inspect diagnostic/collaborator

Responsibility:

- Identify wheat target diagnostics.
- Resolve biome support through the published `BiomeResolver` contract.
- Query `WheatGrowthChancePolicyProvider` for the resolved biome.
- Format read-only diagnostics in the same style as the existing inspect command.

Collaborators:

- `BiomeResolver`
- `WheatGrowthChancePolicyProvider`
- `UnsupportedBiomeException`
- `UnsupportedWheatGrowthPolicyException`
- Existing inspect command routing/composition

Must not:

- Roll `WheatGrowthChanceVariationSource`.
- Call `WheatGrowthService` if that would hide the configured chance needed for diagnostics.
- Mutate any repository or runtime state.
- Add crop-specific fields to biome profiles.
- Make ore presentation depend on crops.

## Out of Scope

Explicitly forbidden work:

- Adding carrot, potato, beetroot, melon, pumpkin, or tree growth support.
- Adding season effects to crop growth.
- Changing growth chance semantics.
- Changing `crop-growth.yml` schema unless a test proves the current schema cannot support diagnostics.
- Adding reload commands.
- Adding action bar feedback.
- Adding public API.
- Adding persistence.
- Adding bonemeal/player-triggered behavior.
- Performing a no-edit audit as part of this slice.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- adding wheat diagnostics would require `ore/drops` to import `crops/growth`;
- command ownership requires a broad command architecture refactor rather than a small composition change;
- required wheat growth provider/service classes do not exist;
- implementation requires changing crop growth runtime behavior;
- implementation requires new YAML schema or migration behavior;
- tests cannot be written without unsafe or unrelated tooling;
- the task would create unused scaffolding.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

If command wiring or plugin runtime changes, also run the focused plugin test used by the repository for command/listener registration.

Inspect changed files:

```bash
git diff --name-status
git diff -- java/app/src/main/java
git diff -- java/app/src/test/java
git diff -- java/app/src/pluginTest/java
```

Boundary checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application

git grep -n "io.github.henriquemichelini.dynamicbiomes.crops" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore || true

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/" || true
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed.
2. Tests added or updated.
3. Commands run and results.
4. Exact user-facing inspect output added.
5. Architecture boundary risks.
6. Deferred work.
