package de.deadzonemc.geschichte;

import de.deadzonemc.charakter.CharakterPayload;
import de.deadzonemc.charakter.Charaktere;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Das Kino-Intro „Flug Q-7“: Jeder baut zuerst seinen Charakter. Sind alle neuen Spieler fertig, startet das Intro für
 * alle gleichzeitig – mit den Charakteren aller, die mitfliegen. Wer zu lange braucht, bekommt sein Intro später allein.
 * Solange Editor und Intro laufen, ist man unverwundbar und Monster ignorieren einen.
 */
public final class Kino {

    private Kino() {
    }

    /** Wartet auf das Intro: UUID → Tick, an dem der Charakter fertig war (-1 = baut noch). */
    private static final Map<UUID, Long> WARTEND = new LinkedHashMap<>();
    /** Sieht gerade das Intro: UUID → spätestes Ende. */
    private static final Map<UUID, Long> IM_KINO = new HashMap<>();
    private static final int HOECHSTENS_WARTEN = 20 * 150;
    private static final int HOECHSTENS_KINO = 20 * 110;

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Kino::tick);
        NeoForge.EVENT_BUS.addListener(Kino::schaden);
        NeoForge.EVENT_BUS.addListener(Kino::ziel);
        NeoForge.EVENT_BUS.addListener(Kino::befehle);
    }

    /** /intro – das Intro noch einmal ansehen (nur man selbst, mit dem eigenen Charakter). */
    private static void befehle(net.neoforged.neoforge.event.RegisterCommandsEvent e) {
        e.getDispatcher().register(net.minecraft.commands.Commands.literal("intro").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (IM_KINO.containsKey(p.getUUID()) || WARTEND.containsKey(p.getUUID())) {
                return 0;
            }
            IM_KINO.put(p.getUUID(), (long) p.server.getTickCount() + HOECHSTENS_KINO);
            wiederholung.add(p.getUUID());
            PacketDistributor.sendToPlayer(p, new CharakterPayload(CharakterPayload.KINO,
                    p.getUUID() + "," + p.getGameProfile().getName()));
            return 1;
        }));
    }

    /** Wer das Intro nur wiederholt, bekommt danach keine Begrüßung zum ersten Akt. */
    private static final java.util.Set<UUID> wiederholung = new java.util.HashSet<>();

    /** Beim ersten Betreten: Intro steht noch aus. */
    static void vormerken(ServerPlayer p) {
        WARTEND.putIfAbsent(p.getUUID(), -1L);
        if (Charaktere.erstellt(p)) {
            bereit(p);
        }
    }

    /** Charakter fertig. */
    public static void bereit(ServerPlayer p) {
        Long t = WARTEND.get(p.getUUID());
        if (t != null && t < 0) {
            WARTEND.put(p.getUUID(), (long) p.server.getTickCount());
        }
        pruefen(p.server);
    }

    public static void abmelden(ServerPlayer p) {
        IM_KINO.remove(p.getUUID());
        Long t = WARTEND.get(p.getUUID());
        if (t != null) {
            WARTEND.put(p.getUUID(), -1L); // kommt er wieder, baut er weiter (Charakter bleibt gespeichert)
        }
    }

    public static boolean geschuetzt(Player p) {
        return WARTEND.containsKey(p.getUUID()) || IM_KINO.containsKey(p.getUUID());
    }

    private static void tick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 20 != 3) {
            return;
        }
        long jetzt = s.getTickCount();
        IM_KINO.entrySet().removeIf(en -> {
            if (jetzt < en.getValue()) {
                return false;
            }
            ServerPlayer p = s.getPlayerList().getPlayer(en.getKey());
            if (p != null && !wiederholung.remove(en.getKey())) {
                begruessen(p);
            }
            return true;
        });
        pruefen(s);
    }

    private static void pruefen(MinecraftServer s) {
        List<ServerPlayer> bereit = new ArrayList<>();
        List<ServerPlayer> offen = new ArrayList<>();
        long aeltester = Long.MAX_VALUE;
        for (var en : WARTEND.entrySet()) {
            ServerPlayer p = s.getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                continue;
            }
            if (en.getValue() >= 0 && Charaktere.erstellt(p) && p.tickCount > 300) {
                bereit.add(p);
                aeltester = Math.min(aeltester, en.getValue());
            } else {
                offen.add(p);
            }
        }
        if (bereit.isEmpty()) {
            return;
        }
        if (offen.isEmpty() || s.getTickCount() - aeltester > HOECHSTENS_WARTEN) {
            starten(s, bereit);
            return;
        }
        String namen = String.join(", ", offen.stream().map(p -> p.getGameProfile().getName()).toList());
        for (ServerPlayer p : bereit) {
            PacketDistributor.sendToPlayer(p, new CharakterPayload(CharakterPayload.WARTEN, namen));
        }
    }

    private static void starten(MinecraftServer s, List<ServerPlayer> gruppe) {
        Fortschritt f = Fortschritt.von(s);
        StringBuilder b = new StringBuilder();
        for (ServerPlayer p : gruppe) {
            if (!b.isEmpty()) {
                b.append(';');
            }
            b.append(p.getUUID()).append(',').append(p.getGameProfile().getName());
        }
        for (ServerPlayer p : gruppe) {
            WARTEND.remove(p.getUUID());
            IM_KINO.put(p.getUUID(), (long) s.getTickCount() + HOECHSTENS_KINO);
            f.merken("intro:" + p.getUUID());
            PacketDistributor.sendToPlayer(p, new CharakterPayload(CharakterPayload.KINO, b.toString()));
        }
    }

    /** Der Client meldet: Intro vorbei (oder übersprungen). */
    public static void fertig(ServerPlayer p) {
        if (IM_KINO.remove(p.getUUID()) != null && !wiederholung.remove(p.getUUID())) {
            begruessen(p);
        }
    }

    private static void begruessen(ServerPlayer p) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 90, 30));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("geschichte.deadzonemc.akt.1")
                .withStyle(ChatFormatting.DARK_RED)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("geschichte.deadzonemc.akt.1.name")
                .withStyle(ChatFormatting.GRAY)));
        p.sendSystemMessage(Component.translatable("geschichte.deadzonemc.intro", p.getName())
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
    }

    private static void schaden(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && geschuetzt(p)) {
            e.setCanceled(true);
        }
    }

    private static void ziel(LivingChangeTargetEvent e) {
        if (e.getNewAboutToBeSetTarget() instanceof ServerPlayer p && geschuetzt(p)) {
            e.setCanceled(true);
        }
    }
}
