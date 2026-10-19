package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Horror-Geräuschkulisse (nur auf dem Client, Klänge aus werkzeuge/atmosphaere.py): Je nach Lage hört man von
 * Zeit zu Zeit ferne Schreie und Schüsse, eine Sirene, Heulen, Klopfen im Haus, Schritte hinter sich, Flüstern im
 * Nebel, ächzendes Metall unter Tage oder Funkrauschen. Nachts und im Nebel viel öfter als am Tag; in der
 * Zuflucht ist Ruhe.
 */
public final class ClientAtmosphaere {

    private ClientAtmosphaere() {
    }

    private static final RandomSource ZUFALL = RandomSource.create();
    private static int warten = 600;

    private record Klang(String name, boolean fern, int gewicht) {
    }

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.isPaused() || mc.level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (--warten > 0) {
            return;
        }
        long zeit = mc.level.getDayTime() % 24000L;
        boolean nacht = zeit > 13000 && zeit < 23000;
        boolean nebel = ClientWetter.art().sicht() < 20; // dichter Nebel, Aschesturm
        BlockPos pos = p.blockPosition();
        boolean drinnen = !mc.level.canSeeSky(pos);
        boolean tief = drinnen && pos.getY() < mc.level.getSeaLevel() - 10;
        int ort = ClientMusik.ort();
        if (ort == 2) {
            warten = 1200; // Zuflucht: Ruhe
            return;
        }
        List<Klang> liste = new ArrayList<>();
        if (tief) {
            liste.add(new Klang("metall", false, 5));
            liste.add(new Klang("klopfen", false, 3));
            liste.add(new Klang("schritte", false, 3));
            liste.add(new Klang("fluestern", false, 1));
        } else {
            liste.add(new Klang("schuesse", true, nacht ? 3 : 4));
            liste.add(new Klang("funk", false, 1));
            liste.add(new Klang("explosion", true, 1));
            liste.add(new Klang("glocke", true, 1));
            liste.add(new Klang("horde", true, nacht ? 4 : 1));
            if (!nacht) {
                liste.add(new Klang("hubschrauber", true, 1));
            }
            if (ort == 1) {
                liste.add(new Klang("autoalarm", true, 2));
            }
            if (nacht) {
                liste.add(new Klang("schrei", true, 5));
                liste.add(new Klang("heulen", true, ort == 1 ? 1 : 4));
                liste.add(new Klang("schritte", false, 2));
                if (drinnen) {
                    liste.add(new Klang("klopfen", false, 4));
                }
            } else {
                liste.add(new Klang("sirene", true, 1));
                liste.add(new Klang("schrei", true, 1));
            }
            if (nebel) {
                liste.add(new Klang("fluestern", false, 6));
                liste.add(new Klang("schritte", false, 2));
            }
        }
        int summe = liste.stream().mapToInt(Klang::gewicht).sum();
        int w = ZUFALL.nextInt(summe);
        Klang k = liste.get(0);
        for (Klang x : liste) {
            w -= x.gewicht();
            if (w < 0) {
                k = x;
                break;
            }
        }
        spielen(mc, p, k);
        // Nächste Pause: nachts/Nebel/unter Tage 30–80 s, tagsüber 2–5 min
        warten = nacht || nebel || tief ? 600 + ZUFALL.nextInt(1000) : 2400 + ZUFALL.nextInt(3600);
    }

    private static void spielen(Minecraft mc, LocalPlayer p, Klang k) {
        Vec3 o;
        float laut = 1.0F;
        if (k.fern()) {
            double wnk = ZUFALL.nextDouble() * Math.PI * 2;
            double ab = 45 + ZUFALL.nextDouble() * 40;
            o = p.position().add(Math.cos(wnk) * ab, 4 + ZUFALL.nextDouble() * 6, Math.sin(wnk) * ab);
        } else if (k.name().equals("schritte") || k.name().equals("fluestern")) {
            // hinter dem Spieler
            Vec3 blick = p.getLookAngle().multiply(1, 0, 1).normalize();
            double ab = k.name().equals("fluestern") ? 2.5 + ZUFALL.nextDouble() * 2 : 7 + ZUFALL.nextDouble() * 5;
            Vec3 seit = new Vec3(-blick.z, 0, blick.x).scale((ZUFALL.nextDouble() - 0.5) * 4);
            o = p.position().subtract(blick.scale(ab)).add(seit).add(0, k.name().equals("fluestern") ? 1.6 : 0, 0);
            laut = k.name().equals("fluestern") ? 0.55F : 0.8F;
        } else {
            double wnk = ZUFALL.nextDouble() * Math.PI * 2;
            double ab = 6 + ZUFALL.nextDouble() * 10;
            o = p.position().add(Math.cos(wnk) * ab, ZUFALL.nextDouble() * 3 - 1, Math.sin(wnk) * ab);
        }
        SoundEvent s = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "atmo." + k.name()));
        mc.getSoundManager().play(new SimpleSoundInstance(s, SoundSource.AMBIENT, laut, 0.92F + ZUFALL.nextFloat() * 0.16F,
                ZUFALL, o.x, o.y, o.z));
    }
}
