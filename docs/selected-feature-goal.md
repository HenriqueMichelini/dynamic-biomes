# Selected Feature Goal

## Ore Drop Multiplier Calculation

Status: Selected

Implement the smallest pure-domain behavior that can interpret an already
validated ore drop multiplier range and choose a multiplier within that range.

This goal is justified by the existing capability-owned `ore-drops.yml`
resource, which contains an iron ore multiplier range. The resource establishes
that ore drop multipliers are intended, but it does not yet justify YAML
parsing, Bukkit event handling, or broader environmental integration.

## Acceptance Behavior

1. Given a multiplier range with equal minimum and maximum values, calculation
   returns that exact multiplier.
2. Given a multiplier range with different minimum and maximum values,
   calculation returns a multiplier within the inclusive range.
3. A multiplier range rejects non-finite values, negative values, and a minimum
   greater than its maximum.
4. Calculation is deterministic when supplied with a deterministic source of
   variation.
5. The behavior is implemented and tested without Bukkit, Paper, YAML, file
   I/O, framework annotations, or runtime composition.

## First Implementation Slice

Add tests first for the acceptance behavior above. Then add only the domain
types required by those tests under `ore/drops/domain`.

No provider, repository, listener, adapter, application service, or
`pluginruntime` change is part of this slice.

## Explicitly Deferred

- Loading or parsing `ore-drops.yml`
- Mapping Minecraft materials or biome names to domain concepts
- Handling block-break or block-place events
- Tracking natural versus player-placed ore
- Applying multipliers to Bukkit item drops
- Biome, season, or ecological-state integration
- Module composition and listener registration

These require separately selected goals with their own observable acceptance
behavior.
