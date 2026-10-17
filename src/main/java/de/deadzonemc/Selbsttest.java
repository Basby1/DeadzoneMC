package de.deadzonemc;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * Automatischer Selbsttest für den Testserver bei GitHub. Nur aktiv, wenn der Server mit
 * {@code -Ddeadzonemc.selbsttest=true} gestartet wurde: führt nacheinander Testbefehle aus und
 * fährt den Server danach herunter.
 */
public final class Selbsttest {

    private static final Logger LOG = LogUtils.getLogger();
    public static final boolean AKTIV = Boolean.getBoolean("deadzonemc.selbsttest");

    private static final List<String> BEFEHLE = List.of(
            "deadzone wetter",
            "deadzone wetter dichter_nebel",
            "deadzone wetter saurer_regen 2",
            "deadzone wetter aschesturm",
            "deadzone wetter klar",
            "deadzone wetter",
            "locate biome deadzonemc:vorstadt",
            "locate biome deadzonemc:toter_wald",
            "locate biome deadzonemc:oedland",
            "locate biome deadzonemc:verseuchtes_moor",
            "locate biome deadzonemc:totes_meer",
            "locate structure deadzonemc:siedlung",
            "locate structure deadzonemc:einzelhaus",
            "place structure deadzonemc:siedlung 0 ~ 0",
            "place structure deadzonemc:einzelhaus 200 ~ 200",
            "place structure deadzonemc:siedlung -300 ~ 300",
            "place structure deadzonemc:siedlung 500 ~ -500",
            "place feature deadzonemc:toter_baum 40 ~ 40",
            "place feature deadzonemc:autowrack 50 ~ 40",
            "place feature deadzonemc:schutt 60 ~ 40",
            "place feature deadzonemc:strommast 70 ~ 40",
            "loot spawn 0 200 0 loot deadzonemc:kiste/haus_kueche",
            "loot spawn 0 200 0 loot deadzonemc:kiste/haus_wohnraum",
            "loot spawn 0 200 0 loot deadzonemc:kiste/haus_schlafzimmer",
            "loot spawn 0 200 0 loot deadzonemc:kiste/haus_keller",
            "loot spawn 0 200 0 loot deadzonemc:kiste/autowrack",
            "give @a deadzonemc:verband",
            "save-all"
    );

    private static int tick = 0;
    private static int naechster = 0;

    private Selbsttest() {
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tick++;
        if (tick < 100 || tick % 30 != 0) {
            return;
        }
        if (naechster < BEFEHLE.size()) {
            String befehl = BEFEHLE.get(naechster++);
            LOG.info("[Selbsttest] /{}", befehl);
            try {
                CommandSourceStack quelle = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(quelle, befehl);
            } catch (Exception e) {
                LOG.error("[Selbsttest] Fehler bei /{}", befehl, e);
            }
            return;
        }
        if (naechster == BEFEHLE.size()) {
            naechster++;
            LOG.info("[Selbsttest] fertig, Server wird beendet");
            server.halt(false);
        }
    }
}
