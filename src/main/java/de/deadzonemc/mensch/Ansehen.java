package de.deadzonemc.mensch;

import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Ansehen eines Spielers bei den Fraktionen (-100 bis 100). Bei -50 oder weniger herrscht Krieg. */
public final class Ansehen {

    public static final int KRIEG = -50;

    private Ansehen() {
    }

    public static int holen(Player p, Fraktion f) {
        return p.getData(ModRegistry.ANSEHEN).getOrDefault(f.id(), 0);
    }

    public static boolean krieg(Player p, Fraktion f) {
        return holen(p, f) <= KRIEG;
    }

    public static void aendern(Player p, Fraktion f, int um) {
        Map<String, Integer> neu = new HashMap<>(p.getData(ModRegistry.ANSEHEN));
        int alt = neu.getOrDefault(f.id(), 0);
        int wert = Math.max(-100, Math.min(100, alt + um));
        neu.put(f.id(), wert);
        p.setData(ModRegistry.ANSEHEN, neu);
        Component name = Component.translatable("fraktion.deadzonemc." + f.id());
        if (alt > KRIEG && wert <= KRIEG) {
            p.displayClientMessage(Component.translatable("fraktion.deadzonemc.krieg", name).withStyle(ChatFormatting.DARK_RED), false);
        } else if (alt <= KRIEG && wert > KRIEG) {
            p.displayClientMessage(Component.translatable("fraktion.deadzonemc.frieden", name).withStyle(ChatFormatting.GREEN), false);
        } else if (alt < 50 && wert >= 50) {
            p.displayClientMessage(Component.translatable("fraktion.deadzonemc.freund", name).withStyle(ChatFormatting.GOLD), false);
        }
    }
}
