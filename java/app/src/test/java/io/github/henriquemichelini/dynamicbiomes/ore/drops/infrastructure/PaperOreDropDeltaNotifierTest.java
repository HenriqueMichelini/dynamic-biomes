package io.github.henriquemichelini.dynamicbiomes.ore.drops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class PaperOreDropDeltaNotifierTest {

    @Test
    void sendsPositiveDeltaFeedback() {
        RecordingActionBarNotifier actionBarNotifier =
            new RecordingActionBarNotifier();
        PaperOreDropDeltaNotifier notifier = new PaperOreDropDeltaNotifier(
            actionBarNotifier
        );

        notifier.notifyDelta(null, Material.IRON_ORE, 3);

        assertEquals(
            List.of(
                new ActionBarNotification(
                    "+3 iron ore extra!",
                    TextColor.color(0x90EE90),
                    OreDropDeltaTone.POSITIVE
                )
            ),
            actionBarNotifier.notifications
        );
    }

    @Test
    void sendsNegativeDeltaFeedback() {
        RecordingActionBarNotifier actionBarNotifier =
            new RecordingActionBarNotifier();
        PaperOreDropDeltaNotifier notifier = new PaperOreDropDeltaNotifier(
            actionBarNotifier
        );

        notifier.notifyDelta(null, Material.DIAMOND_ORE, -1);

        assertEquals(
            List.of(
                new ActionBarNotification(
                    "-1 diamond ore",
                    NamedTextColor.RED,
                    OreDropDeltaTone.NEGATIVE
                )
            ),
            actionBarNotifier.notifications
        );
    }

    @Test
    void doesNotSendZeroDeltaFeedback() {
        RecordingActionBarNotifier actionBarNotifier =
            new RecordingActionBarNotifier();
        PaperOreDropDeltaNotifier notifier = new PaperOreDropDeltaNotifier(
            actionBarNotifier
        );

        notifier.notifyDelta(null, Material.IRON_ORE, 0);

        assertEquals(List.of(), actionBarNotifier.notifications);
    }

    private static final class RecordingActionBarNotifier
        implements OreDropActionBarNotifier {
        private final List<ActionBarNotification> notifications =
            new ArrayList<>();

        @Override
        public void send(
            Player player,
            String message,
            TextColor color,
            OreDropDeltaTone tone
        ) {
            notifications.add(new ActionBarNotification(message, color, tone));
        }
    }

    private record ActionBarNotification(
        String message,
        TextColor color,
        OreDropDeltaTone tone
    ) {}
}
