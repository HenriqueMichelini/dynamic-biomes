---
id: DB-CROPS-013
type: architecture
bounded_context: crops
capability: growth
layer: n/a
status: Ready
expected_commit: "refactor(crops): generalize wheat growth policy model"
---

# Goal Card: Generalize crop growth policy model while preserving wheat behavior

## Status

Ready

## Goal

Refactor the current wheat-specific crop growth model into a crop-growth model that can support multiple crop kinds later, while preserving the existing wheat runtime behavior exactly.

## Why Now

The wheat natural-growth path is now implemented, runtime-wired, manually validated, season-aware, and inspectable. The next feature direction is broader crop support, but adding another crop directly on top of wheat-specific names would either duplicate the pipeline or force a larger mixed refactor later. This card performs only the smallest preparatory refactor needed before adding a second crop kind.

Do not justify additional abstractions beyond the concrete need to support another configured crop after wheat.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/WheatGrowthService.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/WheatGrowthInspectDiagnostic.java`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- Relevant tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth`
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java`

## Current State

- Wheat natural growth is controlled by a configured base chance and optional season-keyed factors.
- The runtime path is wired through `PaperWheatGrowthListener -> WheatGrowthService -> WheatGrowthChancePolicy`.
- `YamlWheatGrowthChancePolicyProvider` reads `crop-growth.yml` for `wheat.growth-chance` and optional `wheat.seasonal-factors`.
- `/dynamicbiomes inspect` reports wheat growth diagnostics, including current season, seasonal factor/default, effective chance, and cancellation status.
- The naming and ports are still wheat-specific, which would encourage duplication when adding carrots, potatoes, or beetroots.

## Architectural Boundary

- Bounded context: `crops`
- Capability: `growth`
- Layer: cross-layer refactor within `crops/growth` plus existing pluginruntime composition references

Rules:

- Preserve the existing dependency direction from `ARCHITECTURE.md`.
- Domain remains pure Java: no Bukkit/Paper/YAML/file I/O/framework annotations.
- Application may coordinate published biome and season ports but must not import Bukkit/Paper events.
- Infrastructure owns Bukkit/Paper and YAML translation.
- Pluginruntime may update constructor/wiring references only for composition.
- Do not make `ore` depend on `crops`.
- Do not add empty packages, placeholder classes, or speculative extension points.

## Acceptance Behavior

1. Given the current default `crop-growth.yml`, when wheat naturally grows in a configured biome and season, then the runtime allow/cancel behavior is unchanged from before this refactor.
2. Given `/dynamicbiomes inspect` targets wheat, then the diagnostic output remains semantically unchanged: it still reports base chance, current season, seasonal factor/default, effective chance, and fallback status.
3. Given unsupported biome or unsupported wheat policy, then vanilla fallback behavior remains unchanged.
4. Given malformed or invalid crop-growth YAML, then provider/parser failures still propagate as before; they must not be swallowed by pluginruntime.
5. Given existing focused crop tests, then tests may be renamed or updated, but equivalent behavior coverage must remain.
6. Given the project still only supports wheat at runtime, then no new crop kind is exposed, configured, inspected, or registered by this card.

## TDD Plan

1. Add or update the smallest relevant regression test first, preferably asserting unchanged wheat behavior through the refactored API.
2. Confirm the test fails for the expected naming/API mismatch when practical.
3. Rename or extract production types in the smallest safe increments.
4. Update tests and pluginruntime references as needed.
5. Run focused crop tests.
6. Run plugin runtime tests affected by constructor/wiring changes.
7. Run full verification.

## Expected Files

Production, likely renamed or updated:

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/*`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/*`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/*`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/*`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/DynamicBiomes.java`
- `ARCHITECTURE.md` if implemented names/structure change

Tests, likely renamed or updated:

- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain/*`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application/*`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/infrastructure/*`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/presentation/*`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/lifecycle/infrastructure/*`
- `java/app/src/pluginTest/java/io/github/henriquemichelini/dynamicbiomes/pluginruntime/DynamicBiomesPluginTest.java`

These are expected files, not permission to create extra scaffolding.

## Responsibility / Collaboration Notes

Refactor target:

- Prefer crop-growth vocabulary such as crop growth chance, crop growth policy, crop growth seasonal factor, crop growth decision, and crop growth policy provider.
- Keep wheat as the only supported configured crop for now.
- Keep `crop-growth.yml` semantically unchanged for wheat.

Must not:

- Add carrots, potatoes, beetroots, stems, saplings, trees, or animals.
- Add a public plugin API.
- Add reload behavior.
- Change probability semantics.
- Change season lookup semantics.
- Introduce a generic “effect” or biome-owned crop rule model.

## Out of Scope

Explicitly forbidden work:

- Adding a second crop kind.
- Changing the YAML schema beyond names required by safe internal refactoring.
- Changing default crop balance values.
- Adding new Bukkit listeners or listener registration.
- Changing `plugin.yml`.
- Adding database persistence.
- Adding commands beyond preserving current inspect behavior.
- Adding no-edit audit output unless separately requested.

## Stop Conditions

Stop and report instead of editing if:

- the task conflicts with `ARCHITECTURE.md`;
- the task conflicts with `AGENTS.md`;
- the refactor requires changing runtime behavior to pass tests;
- preserving the existing YAML shape is not possible without a larger migration;
- the implementation would create unused scaffolding or placeholder multi-crop APIs;
- tests reveal unclear current behavior that is not covered by this card;
- pluginruntime changes would exceed constructor/wiring updates;
- a second crop must be added to justify the refactor.

## Verification

Run:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Also run focused tests affected by the refactor, including crop growth tests and pluginruntime tests if constructor/wiring changes are made.

Run architecture-sensitive checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/domain java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/growth/application

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/"

git grep -n "io.github.henriquemichelini.dynamicbiomes.crops" -- java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/ore || true
```

If a command cannot be run, report why.

## Report Back

After implementation, report:

1. Files changed, including renamed files.
2. Tests added, renamed, or updated.
3. Whether wheat runtime behavior remained unchanged.
4. Whether `crop-growth.yml` shape remained compatible.
5. Commands run and results.
6. Architecture boundary risks.
7. Deferred work, especially the first concrete second-crop slice.
