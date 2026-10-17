package de.deadzonemc.welt;

import com.mojang.brigadier.context.CommandContext;
import de.deadzonemc.welt.plan.Hoehen;
import de.deadzonemc.welt.plan.Ortsplan;
import de.deadzonemc.welt.plan.PlanSpeicher;
import de.deadzonemc.welt.plan.Siedlung;
import de.deadzonemc.welt.plan.SiedlungsTyp;
import de.deadzonemc.welt.plan.Siedlungen;
import de.deadzonemc.welt.stadt.KachelStruktur;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;

/** Testbefehle für Orte: /deadzone orte und /deadzone hin. */
public final class Ortsbefehle {

    private Ortsbefehle() {
    }

    public static void registrieren(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deadzone")
                .requires(q -> q.hasPermission(2))
                .then(Commands.literal("orte").executes(Ortsbefehle::orte))
                .then(Commands.literal("hin").executes(ctx -> hin(ctx, SiedlungsTyp.DORF))
                        .then(Commands.literal("dorf").executes(ctx -> hin(ctx, SiedlungsTyp.DORF)))
                        .then(Commands.literal("kleinstadt").executes(ctx -> hin(ctx, SiedlungsTyp.KLEINSTADT)))
                        .then(Commands.literal("grossstadt").executes(ctx -> hin(ctx, SiedlungsTyp.GROSSSTADT)))));
    }

    /** Ob die Siedlung wirklich gebaut wird (nicht im Wasser usw.). */
    public static boolean gueltig(ServerLevel level, Siedlung s) {
        if (!s.typ().wirdGebaut()) {
            return true;
        }
        Hoehen h = KachelStruktur.hoehen(level.getChunkSource().getGenerator(), level, level.getChunkSource().randomState());
        Ortsplan plan = PlanSpeicher.fertig(level.getSeed(), s, h, level.getSeaLevel());
        return plan.gueltig;
    }

    /** Die nächste gültige Siedlung eines Typs, oder null. */
    public static Siedlung naechste(ServerLevel level, SiedlungsTyp typ, int x, int z) {
        List<Siedlung> liste = Siedlungen.naechste(level.getSeed(), typ, x, z, 3);
        for (Siedlung s : liste) {
            if (gueltig(level, s)) {
                return s;
            }
        }
        return null;
    }

    public static String richtung(int vonX, int vonZ, int nachX, int nachZ) {
        double winkel = Math.toDegrees(Math.atan2(nachX - vonX, -(nachZ - vonZ)));
        String[] namen = {"Norden", "Nordosten", "Osten", "Südosten", "Süden", "Südwesten", "Westen", "Nordwesten"};
        int i = (int) Math.round(((winkel % 360) + 360) % 360 / 45.0) % 8;
        return namen[i];
    }

    private static int orte(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack q = ctx.getSource();
        ServerLevel level = q.getServer().overworld();
        Vec3 p = q.getPosition();
        int x = (int) p.x;
        int z = (int) p.z;
        q.sendSuccess(() -> Component.literal("Orte in der Nähe:").withStyle(ChatFormatting.GOLD), false);
        int gezeigt = 0;
        for (Siedlung s : Siedlungen.naechste(level.getSeed(), SiedlungsTyp.DORF, x, z, 2)) {
            if (gezeigt >= 5) {
                break;
            }
            if (!gueltig(level, s)) {
                continue;
            }
            gezeigt++;
            q.sendSuccess(() -> zeile(s, x, z, ""), false);
        }
        for (SiedlungsTyp typ : new SiedlungsTyp[]{SiedlungsTyp.KLEINSTADT, SiedlungsTyp.GROSSSTADT}) {
            List<Siedlung> liste = Siedlungen.naechste(level.getSeed(), typ, x, z, 2);
            if (!liste.isEmpty()) {
                Siedlung s = liste.get(0);
                q.sendSuccess(() -> zeile(s, x, z, " (wird noch gebaut)"), false);
            }
        }
        return 1;
    }

    private static Component zeile(Siedlung s, int x, int z, String zusatz) {
        String typ = switch (s.typ()) {
            case DORF -> "Dorf";
            case KLEINSTADT -> "Kleinstadt";
            case GROSSSTADT -> "Großstadt";
        };
        return Component.literal(String.format(Locale.GERMANY, "  %s %s – %d Blöcke nach %s [%d, %d]%s",
                typ, s.name(), (int) s.abstand(x, z), richtung(x, z, s.x(), s.z()), s.x(), s.z(), zusatz));
    }

    private static int hin(CommandContext<CommandSourceStack> ctx, SiedlungsTyp typ) {
        CommandSourceStack q = ctx.getSource();
        ServerPlayer spieler = q.getPlayer();
        if (spieler == null) {
            q.sendFailure(Component.literal("Nur für Spieler."));
            return 0;
        }
        ServerLevel level = q.getServer().overworld();
        Siedlung s = naechste(level, typ, spieler.getBlockX(), spieler.getBlockZ());
        if (s == null) {
            q.sendFailure(Component.literal("Kein Ort gefunden."));
            return 0;
        }
        level.getChunk(s.x() >> 4, s.z() >> 4);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.x(), s.z());
        spieler.teleportTo(level, s.x() + 0.5, y + 1, s.z() + 0.5, spieler.getYRot(), spieler.getXRot());
        q.sendSuccess(() -> Component.literal("Willkommen in " + s.name() + "."), false);
        return 1;
    }
}
