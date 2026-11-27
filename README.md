# DynamicBiomes

DynamicBiomes is a Paper 1.21.4 plugin for biome- and season-aware Minecraft
gameplay. It adjusts ore drops, crop growth and harvests, and tree generation.
Server operators can also configure seasonal breeding chances for farm animals.

## Build and install

Use Java 21. From the repository root, run:

```sh
cd java
./gradlew test build --no-daemon
```

Copy `java/app/build/libs/dynamic-biomes-1.0.0.jar` to the Paper server's
`plugins/` directory and start the server. For a local Docker test server, run
`./server restart --plugin` from the repository root and connect to
`localhost:25566`.

## Commands

Use `/dynamicbiomes` or its shorter alias `/db`.

| Command | Behavior |
| --- | --- |
| `/db season` | Shows the current season. Works from the console. |
| `/db biome` | Shows configured biome temperature, humidity, and soil fertility, along with available regional sky observation at the player's position. |
| `/db inspect` | Shows the targeted crop's growth and yield factors or ore's drop policy and recorded origin. |
| `/db diagnostics` | Shows ore-origin write counts, failures, approximate p95 duration, and the in-memory origin count when loaded. Works from the console. Requires `dynamicbiomes.admin.diagnostics`. |
| `/db season set minecraft:winter` | Sets and persists a configured season. Requires `dynamicbiomes.admin.season`. |
| `/db reload` | Applies edited configuration. Requires `dynamicbiomes.admin.reload`. A failed reload keeps the current configuration active. |

Administrative permissions default to server operators. Biome and inspect
commands require a player. Inspect also requires looking at a supported block.

Diagnostics cover the time since plugin startup. The approximate p95 is an
upper bound from a fixed histogram of synchronous YAML rewrites, including
failed attempts. It shows no data until the first write attempt. Reading
diagnostics uses the repository's in-memory state and leaves unloaded origins
unloaded. Metrics survive `/db reload` and reset on a full plugin restart.

## Configuration

DynamicBiomes copies missing defaults to `plugins/dynamic-biomes/` on startup. Existing
files keep their settings. Edit the feature's YAML and run `/db reload`.
Unrecognized settings in `crop-growth.yml` or `crop-yields.yml` fail validation.

| File | Settings |
| --- | --- |
| `biome-profiles.yml` | Supported biome profiles with temperature, humidity, and fertility. Forest is bundled. |
| `season-cycle.yml` | Ordered calendar and optional automatic season advancement. Advancement is disabled by default. |
| `season-profiles.yml` | Climate adjustments for each season in the calendar. |
| `ore-drops.yml` | Ore multipliers by biome, ore, and season. Silk Touch and player-placed ore retain vanilla drops. |
| `crop-profiles.yml` | Environmental preferences for wheat, carrots, potatoes, and beetroot. |
| `crop-growth.yml`, `crop-yields.yml` | Crop-wide tuning and optional season factors. Existing rules for a specific biome and crop take precedence. |
| `tree-growth.yml` | Chances of eligible tree generation by biome and season. The bundled forest rule leaves spring at its vanilla rate. |
| `animal-breeding.yml` | Optional chances for player-fed cows, sheep, pigs, and chickens by season. The bundled file leaves breeding vanilla. |

A successful reload preserves the current season and restarts the configured
advancement countdown. To remove the current season from the calendar, select a
season that will remain in it before reloading. The plugin manages its persisted
season and ore origins in `season-state.yml` and `ore-origins.yml`.

Paper 1.21.4 can report dispenser bone meal as natural tree growth. A crop
growth event can also look the same when bone meal advances just one stage.
Those events can be affected by the configured growth rules. Breeding chance
is evaluated after feeding, so a failed breeding attempt still uses the food.
