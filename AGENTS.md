# AGENTS.md — DynamicBiomes Agent Guide

This file gives coding agents project-specific instructions for working on DynamicBiomes. Follow it before editing code. When this file conflicts with `ARCHITECTURE.md`, `ARCHITECTURE.md` is authoritative.

## Skill Ecosystem

Use only `.codex/skills/` for this repository. Do not use `.kimi-code/skills/`.

Canonical skills:

- `create-goal-card` — creates or updates a Goal Card only; does not implement.
- `implement-card` — implements exactly one selected Goal Card only.
- `reverify-implementation` — audits a completed implementation only; does not edit files by default.

## Project Overview

DynamicBiomes is a Java/Paper Minecraft plugin organized as a DDD-inspired modular monolith.

Primary project path:

```text
java/app
```

Important files:

```text
ARCHITECTURE.md                         # Source of truth for architecture and boundaries
java/app/build.gradle.kts               # Application Gradle build
java/gradle/libs.versions.toml          # Version catalog
java/app/src/main/java                  # Production code
java/app/src/main/resources             # Plugin resources, including plugin.yml and ore-drops.yml
java/app/src/test/java                  # Tests, including architecture tests
```

## Non-Negotiable Workflow

1. Read `ARCHITECTURE.md` before implementing any task.
2. Inspect the current repository state before editing. Do not assume a target class/package exists just because it appears in `ARCHITECTURE.md`.
3. Implement one small slice at a time.
4. Follow TDD whenever behavior is added or changed:
   - write or update the smallest relevant test first;
   - confirm it fails for the expected reason when practical;
   - implement the smallest production change;
   - run the relevant tests.
5. Do not create unused scaffolding, empty packages, placeholder contracts, or speculative layers.
6. Preserve runtime behavior unless the current task explicitly requires changing it.
7. After implementation, run verification commands and report exact results.
8. If you modified any files/styles/structures/configurations/workflows/... mentioned in `AGENTS.md` or `ARCHITECTURE.md`, you MUST update the corresponding document to keep it up to date.

## Architecture Quick Reference

The codebase follows this package shape:

```text
root package
└── bounded context
    └── capability
        └── architectural layer
```

Layers are created only when needed. Do not create empty layer packages preemptively.

Layer responsibilities, dependency direction, published language, value object rules, domain events, and test parity are defined in `ARCHITECTURE.md`:

- Layer responsibilities: Section 5
- Dependency direction: Section 3
- Published language: Section 4
- Value object rules: Section 10
- Domain events: Section 13
- Test package parity: Section 16
- Optional `presentation/` layer: Section 5.4

Operational reminders not restated in `ARCHITECTURE.md`:

- `pluginruntime` may import contexts only for startup/composition.
- No package outside `pluginruntime` may import `pluginruntime`.
- Do not add `ModuleComposer` or runtime wiring unless the task explicitly requires it.
- Listener registration is not added unless the task explicitly requires runtime wiring.
- Runtime ore drop modification must not be shipped unless anti-exploit origin tracking is present in the runtime path.
- Do not implement the Minecraft Fortune algorithm unless a task explicitly asks for it. Current drop quantity logic treats vanilla/Fortune quantity as already computed.
- Lombok compile-time annotations are allowed in domain for boilerplate reduction and invariant enforcement; runtime framework annotations are forbidden there. See `ARCHITECTURE.md` Section 5.1.

## Context Dependency Rules

See `ARCHITECTURE.md` Section 3 and Section 4.

## Current Ore Context Rules

See `ARCHITECTURE.md` Section 8.3 for structural ownership.

Additional runtime rules:

- `ore/origin` owns `OreOrigin`, `OreOriginRepository`, origin tracking, cleanup, and origin persistence.
- `ore/drops` may consume origin eligibility but must not own origin tracking.
- `PaperOrePlaceListener` belongs in `ore/origin/infrastructure`.
- `PaperOreBreakListener` belongs in `ore/drops/infrastructure`.

## Configuration and Persistence Rules

See `ARCHITECTURE.md` Section 11 for persistence and configuration architecture.

Resource ownership:

```text
ore-drops.yml        -> ore/drops
biome-profiles.yml   -> biome/profile, when introduced
season-profiles.yml  -> seasons/profile, when introduced
```

Additional rules:

- Raw YAML must not leak into domain.
- Do not introduce a generic domain `ConfigurationProvider`.
- Use typed provider ports owned by the capability, for example `OreDropPolicyProvider`.
- YAML provider implementations belong in infrastructure.
- Repository ports belong to the owning domain/capability.
- YAML/file repository implementations belong in infrastructure.
- Do not create shared YAML utilities unless there is real duplication that justifies them.

## Testing Rules

See `ARCHITECTURE.md` Section 16 for test architecture.

Test package parity example:

```text
src/main/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain/OreDropQuantityCalculator.java
src/test/java/io/github/henriquemichelini/dynamicbiomes/ore/drops/domain/OreDropQuantityCalculatorTest.java
```

Additional rules:

- Domain tests must use pure Java/domain objects only.
- Domain tests must not require Bukkit, YAML, file I/O, or database access.
- Application tests use real domain objects and in-memory fakes/stubs for ports.
- Infrastructure tests may use temporary files or Bukkit/Paper mocks when safe.
- If safe Bukkit/Paper event testing is not available, explicitly report that event-level tests were deferred and rely on architecture/build tests.
- Architecture tests must stay aligned with `ARCHITECTURE.md`; do not make them stricter than the document.

## Verification Commands

Run after each implementation:

```bash
git status --short
git diff --stat
git diff --check
cd java && ./gradlew test build --no-daemon
```

Inspect changed files:

```bash
git diff --name-status
git diff -- src/main/java
git diff -- src/main/resources
git diff -- src/test/java
```

Useful architecture checks:

```bash
git grep -nE "org\.bukkit|io\.papermc|org\.yaml|snakeyaml|java\.nio\.file|java\.io\.File|InputStream|OutputStream|Reader|Writer" -- java/app/src/main/java

git grep -n "pluginruntime" -- java/app/src/main/java | grep -v "/pluginruntime/"
```

JAR/resource check:

```bash
cd java && ./gradlew build --no-daemon
jar tf app/build/libs/*.jar | sort | grep -E "plugin.yml|ore-drops.yml|biomes.yml|DynamicBiomes"
```

## Agent Task Format

Prefer `/goal` for implementation tasks.

A good task must include:

- the exact selected slice;
- files/packages likely to change;
- explicit acceptance behavior;
- TDD instructions;
- forbidden changes;
- verification commands;
- a required final report.

Do not respond to vague instructions like “start the next task” by guessing broadly. First inspect the current repository and propose the next smallest justified task.

## Standard Implementation Guard

Before editing, verify the task against this checklist:

```text
- Does this task match ARCHITECTURE.md?
- Is this change justified by current code or an explicit selected feature goal?
- Am I avoiding unused scaffolding?
- Am I preserving the correct bounded-context ownership?
- Are domain/application/infrastructure responsibilities respected?
- Are tests added or updated first?
- Is runtime behavior unchanged unless explicitly requested?
```

If any answer is unclear, stop and report the uncertainty before editing.

## Standard No-Edit Audit Prompt

After implementation, perform or request a no-edit audit:

```text
Review the current git diff against ARCHITECTURE.md.

Do not edit files.

Check:
- no production package, class, port, provider, repository, listener, adapter, or value object was created unless required by this task;
- no empty or speculative DDD scaffolding was added;
- no Bukkit/Paper/YAML/file I/O/framework annotations leaked into domain;
- application packages do not import Bukkit events/listeners;
- pluginruntime is still only composition/lifecycle-related;
- upstream contexts do not depend on downstream contexts;
- downstream contexts do not import upstream infrastructure;
- tests mirror production package structure where relevant;
- runtime behavior was not changed unless explicitly required by the task;
- `cd java && ./gradlew test build --no-daemon` passes or the reason it was not run is reported.

Report findings by severity: HIGH, MEDIUM, LOW. Include exact files and lines when possible.
```

## Commit Guidance

Keep commits small and scoped to one slice.

Examples:

```bash
git commit -m "test: tighten architecture boundary rules"
git commit -m "feat(ore): apply drop multiplier to quantities"
git commit -m "feat(ore): track ore origin eligibility"
git commit -m "feat(ore): persist tracked ore origins in yaml"
git commit -m "feat(ore): record placed ore blocks"
```

Do not combine architecture, domain, infrastructure, and runtime wiring changes in one commit unless explicitly required for atomic correctness.

## Continuing Work

If continuing an existing feature, inspect the repository first. Do not assume a slice is still pending. Verify current files and tests before proposing the next task.

When working on this repository:
1. Follow the user’s explicit task.
2. Follow AGENTS.md for workflow and verification.
3. Follow ARCHITECTURE.md for design boundaries.
4. If the user task conflicts with ARCHITECTURE.md, stop and report the conflict before editing.
