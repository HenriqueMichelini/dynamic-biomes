This listener currently has several reasons to change:

1. **Paper event adaptation changes**

It depends on `BlockBreakEvent`, `Block`, `Player`, `Material`, `ItemStack`, Bukkit sounds, action bars, and world drops. That is expected for an infrastructure listener.

2. **Ore identification changes**

It maps `Material` to `OreKind` through `PaperOreMaterialMapper`.

3. **Vanilla drop interpretation changes**

It decides whether the vanilla drop should be ignored, whether the ore block itself would drop, and how much vanilla fortune quantity exists.

```java
VanillaDropScan vanillaDropScan = scanVanillaDrops(vanillaDrops, material);

if (vanillaDropScan.wouldDropOriginalBlock()) return;

int vanillaFortuneQuantity = vanillaDropScan.quantity();
```

This is business-adjacent logic. It is currently hidden inside the listener.

4. **Drop replacement behavior changes**

It decides:

```java
event.setDropItems(false);

if (quantity <= 0) return;

ItemStack boostedDrop = vanillaDrops.iterator().next().clone();
boostedDrop.setAmount(quantity);

block.getWorld().dropItemNaturally(block.getLocation(), boostedDrop);
```

That is Paper-specific mutation logic, but it is still a separate responsibility from listening to events.

5. **User feedback changes**

It formats the action bar message, chooses the color, chooses the tone, and maps tone to sound.

```java
formatDeltaMessage(delta, material)
deltaColor(delta)
deltaTone(delta)
soundFor(tone)
```

This is clearly not the listener’s core responsibility.

6. **Origin cleanup lifecycle changes**

The listener is also responsible for always clearing tracked origin state in `finally`.

```java
finally {
    originTracking.clearTrackedOrigin(position);
}
```

That cleanup requirement is legitimate here, but it contributes to the listener being an orchestration point.

So the better diagnosis is:

> `PaperOreBreakListener` is currently an infrastructure orchestrator with embedded policies for vanilla drop scanning, drop replacement, and player feedback. It does not catastrophically violate SRP, but it is accumulating multiple reasons to change and should be split before it grows further.

The listener’s ideal responsibility should be closer to:

> Convert a Paper `BlockBreakEvent` into an application/infrastructure command, delegate the actual ore-break handling, then apply the result to Paper.

For this project style, I would not move everything into the domain. Some parts are Paper-specific and should stay infrastructure. But they should be separate infrastructure collaborators.

A cleaner split would be:

```java
PaperOreBreakListener
```

Only handles the Bukkit event and delegates.

```java
PaperOreBreakHandler
```

Coordinates the ore break flow: identify ore, create `BlockPosition`, scan vanilla drops, call `OreDropService`, clear origin tracking.

```java
PaperVanillaDropScanner
```

Handles:

```java
Collection<ItemStack> vanillaDrops
Material blockMaterial
```

and returns something like:

```java
record PaperVanillaDropResult(
    boolean empty,
    boolean wouldDropOriginalBlock,
    int quantity,
    Optional<ItemStack> representativeDrop
) {}
```
```java
PaperOreDropReplacer
```

Handles:

```java
event.setDropItems(false);
world.dropItemNaturally(...);
```
```java
OreDropDeltaNotifier
```

Handles action bar message, color, tone, and sound.

The functional interface is a good test seam, but it is too low-level. You already noticed the real domain concept: this is not merely an “action bar notifier”; it is a **drop delta notifier**.

This part:

```java
@FunctionalInterface
interface OreDropActionBarNotifier {
    void send(
        Player player,
        String message,
        TextColor color,
        OreDropDeltaTone tone
    );
}
```

leaks formatting responsibilities back into the listener. A better shape would be:

```java
interface OreDropDeltaNotifier {
    void notify(Player player, OreDropDelta delta);
}
```

Then the notifier owns message formatting, color choice, and sound choice.

The current `VanillaDropScan` is also a good candidate to extract. This method:

```java
private static VanillaDropScan scanVanillaDrops(
    Collection<ItemStack> drops,
    Material blockMaterial
)
```

is pure and testable, which is good. But because it expresses a decision important to ore drops, keeping it inside the listener hides policy inside event glue code.

A better extracted version:

```java
final class PaperVanillaDropScanner {

    PaperVanillaDropScan scan(
        Collection<ItemStack> drops,
        Material blockMaterial
    ) {
        boolean wouldDropOriginalBlock = false;
        int quantity = 0;
        ItemStack representativeDrop = null;

        for (ItemStack drop : drops) {
            if (representativeDrop == null) {
                representativeDrop = drop.clone();
            }

            if (drop.getType() == blockMaterial) {
                wouldDropOriginalBlock = true;
            }

            quantity += drop.getAmount();
        }

        return new PaperVanillaDropScan(
            drops.isEmpty(),
            wouldDropOriginalBlock,
            quantity,
            Optional.ofNullable(representativeDrop)
        );
    }
}
```

Then the listener becomes more like:

```java
@EventHandler(ignoreCancelled = true)
public void onBlockBreak(BlockBreakEvent event) {
    oreBreakHandler.handle(event);
}
```

That is usually what you want for Paper listeners: thin adapters.

However, I would avoid overcorrecting. Do not split this into ten abstractions just because SRP exists. The most valuable immediate extractions are:

1. Extract `PaperVanillaDropScanner`.
2. Extract `PaperOreDropDeltaNotifier`.
3. Optionally extract `PaperOreDropReplacer`.
4. Keep `originTracking.clearTrackedOrigin(position)` in the orchestration flow unless you introduce a dedicated handler/service around the full event lifecycle.

The current class is acceptable as an early implementation, especially because most logic is simple and localized. But as soon as you add Silk Touch rules, multiple drop types, config-driven messages, permissions, biome/debug messages, or different sounds, this listener will become brittle.

My recommendation: refactor now only around the two clearest responsibilities: **vanilla drop scanning** and **delta notification**. That gives you better SRP without creating artificial architecture.
