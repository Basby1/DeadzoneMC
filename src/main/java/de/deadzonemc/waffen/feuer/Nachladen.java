package de.deadzonemc.waffen.feuer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Nachladen: dauert je nach Waffe; Schrotflinten und Revolver laden Patrone für Patrone (abbrechbar durch Schießen). */
public final class Nachladen {

    private record Vorgang(ServerPlayer spieler, int slot, long fertig) {
    }

    private static final Map<UUID, Vorgang> LAUFEND = new HashMap<>();

    private Nachladen() {
    }

    public static void empfangen(WaffenPayloads.Laden n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                starten(p);
            }
        });
    }

    public static boolean laedt(ServerPlayer p) {
        return LAUFEND.containsKey(p.getUUID());
    }

    public static void abbrechen(ServerPlayer p) {
        LAUFEND.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new WaffenPayloads.Rueckmeldung(0, -1));
    }

    public static int vorrat(ServerPlayer p, Kaliber k) {
        if (p.isCreative()) {
            return 999;
        }
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof MunitionItem m && m.kaliber == k) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static int nehmen(ServerPlayer p, Kaliber k, int anzahl) {
        if (p.isCreative()) {
            return anzahl;
        }
        int genommen = 0;
        for (int i = 0; i < p.getInventory().getContainerSize() && genommen < anzahl; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof MunitionItem m && m.kaliber == k) {
                int x = Math.min(anzahl - genommen, s.getCount());
                s.shrink(x);
                genommen += x;
            }
        }
        return genommen;
    }

    /** Nachladezeit mit Verzauberung Schnellladen (-15 % je Stufe). */
    private static int dauer(ServerPlayer p, ItemStack s, WaffenArt a) {
        return Math.max(4, Math.round(a.laden * (1 - 0.15F * de.deadzonemc.verzauberung.Zonenverzauberung.stufe(
                p.level().registryAccess(), s, "schnellladen")) * de.deadzonemc.klasse.Klassenvorteile.nachladen(p, s)
                * de.deadzonemc.bossmod.Bossmods.nachladen(s)
                * (de.deadzonemc.gadget.Gadgets.hatGurt(p) ? (de.deadzonemc.klasse.Klassen.hat(p, de.deadzonemc.klasse.Klasse.SCHUETZE) ? 0.65F : 0.8F) : 1F)));
    }

    public static void starten(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof SchusswaffeItem w) || laedt(p)) {
            return;
        }
        WaffenArt a = w.art;
        int mag = s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
        if (mag >= Feuerwaffen.magazinGroesse(s, a)) {
            return;
        }
        if (vorrat(p, a.kaliber) <= 0) {
            p.displayClientMessage(Component.translatable("waffe.deadzonemc.keine_munition",
                    Component.translatable("item.deadzonemc." + a.kaliber.itemId())).withStyle(ChatFormatting.RED), true);
            return;
        }
        int dauer = dauer(p, s, a);
        LAUFEND.put(p.getUUID(), new Vorgang(p, p.getInventory().selected, p.serverLevel().getGameTime() + dauer));
        p.serverLevel().playSound(null, p.blockPosition(), Feuerwaffen.LADEN.get(), SoundSource.PLAYERS, 0.9F,
                a.einzeln ? 1.2F : 1.0F);
        PacketDistributor.sendToPlayer(p, new WaffenPayloads.Rueckmeldung(0, dauer));
    }

    public static void tick(ServerTickEvent.Post event) {
        if (LAUFEND.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Vorgang>> it = LAUFEND.entrySet().iterator();
        java.util.List<ServerPlayer> weiter = new java.util.ArrayList<>();
        while (it.hasNext()) {
            Vorgang v = it.next().getValue();
            ServerPlayer p = v.spieler;
            ItemStack s = p.getMainHandItem();
            if (p.isRemoved() || p.getInventory().selected != v.slot || !(s.getItem() instanceof SchusswaffeItem w)) {
                it.remove();
                continue;
            }
            if (p.serverLevel().getGameTime() < v.fertig) {
                continue;
            }
            it.remove();
            WaffenArt a = w.art;
            int mag = s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
            int platz = Feuerwaffen.magazinGroesse(s, a) - mag;
            int rein = nehmen(p, a.kaliber, a.einzeln ? Math.min(1, platz) : platz);
            s.set(Feuerwaffen.MAGAZIN.get(), mag + rein);
            if (a.einzeln && mag + rein < Feuerwaffen.magazinGroesse(s, a) && vorrat(p, a.kaliber) > 0) {
                p.serverLevel().playSound(null, p.blockPosition(), Feuerwaffen.PATRONE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                weiter.add(p);
            } else {
                p.serverLevel().playSound(null, p.blockPosition(), Feuerwaffen.LADEN_FERTIG.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
                PacketDistributor.sendToPlayer(p, new WaffenPayloads.Rueckmeldung(0, -1));
            }
        }
        for (ServerPlayer p : weiter) {
            ItemStack s = p.getMainHandItem();
            if (s.getItem() instanceof SchusswaffeItem w) {
                int d = dauer(p, s, w.art);
                LAUFEND.put(p.getUUID(), new Vorgang(p, p.getInventory().selected, p.serverLevel().getGameTime() + d));
                PacketDistributor.sendToPlayer(p, new WaffenPayloads.Rueckmeldung(0, d));
            }
        }
    }
}
