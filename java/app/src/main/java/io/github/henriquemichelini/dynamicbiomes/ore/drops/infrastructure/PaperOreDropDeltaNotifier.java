package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

final class PaperOreDropDeltaNotifier implements OreDropDeltaNotifier {

    private static final TextColor POSITIVE_DELTA_COLOR = TextColor.color(
        0x90EE90
    );
    private static final TextColor NEGATIVE_DELTA_COLOR = NamedTextColor.RED;

    private final OreDropActionBarNotifier actionBarNotifier;

    PaperOreDropDeltaNotifier() {
        this((player, message, color, tone) -> {
            player.sendActionBar(Component.text(message, color));
            player.playSound(player.getLocation(), soundFor(tone), 1.0f, 1.0f);
        });
    }

    PaperOreDropDeltaNotifier(
        @NonNull OreDropActionBarNotifier actionBarNotifier
    ) {
        this.actionBarNotifier = actionBarNotifier;
    }

    @Override
    public void notifyDelta(
        Player player,
        Material material,
        int delta
    ) {
        if (delta == 0) {
            return;
        }

        actionBarNotifier.send(
            player,
            formatDeltaMessage(delta, material),
            deltaColor(delta),
            deltaTone(delta)
        );
    }

    private static String formatDeltaMessage(int delta, Material material) {
        String oreName = material.name().toLowerCase().replace('_', ' ');
        if (delta > 0) {
            return "+" + delta + " " + oreName + " extra!";
        }
        return delta + " " + oreName;
    }

    private static TextColor deltaColor(int delta) {
        return delta > 0 ? POSITIVE_DELTA_COLOR : NEGATIVE_DELTA_COLOR;
    }

    private static OreDropDeltaTone deltaTone(int delta) {
        return delta > 0
            ? OreDropDeltaTone.POSITIVE
            : OreDropDeltaTone.NEGATIVE;
    }

    private static Sound soundFor(OreDropDeltaTone tone) {
        return switch (tone) {
            case POSITIVE -> Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            case NEGATIVE -> Sound.ITEM_WOLF_ARMOR_REPAIR;
        };
    }
}

interface OreDropDeltaNotifier {
    void notifyDelta(Player player, Material material, int delta);
}

@FunctionalInterface
interface OreDropActionBarNotifier {
    void send(
        Player player,
        String message,
        TextColor color,
        OreDropDeltaTone tone
    );
}

enum OreDropDeltaTone {
    POSITIVE,
    NEGATIVE,
}
