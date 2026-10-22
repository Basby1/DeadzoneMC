package de.deadzonemc.client.charakter;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.charakter.CharakterPayload;
import de.deadzonemc.client.ClientKlasse;
import de.deadzonemc.client.IntroScreen;
import de.deadzonemc.client.KlassenScreen;
import de.deadzonemc.client.kino.ClientKino;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Aussehen aller Spieler auf dem Client: Codes, fertige Skin-Texturen, Editor-Ablauf. */
public final class ClientCharakter {

    private ClientCharakter() {
    }

    private static final Map<UUID, String> CODES = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTUREN = new HashMap<>();
    /** Der Server wartet auf unseren Charakter (erster Start). */
    static boolean editorAusstehend;
    /** Auf wen gerade gewartet wird (leer = auf niemanden). */
    static String wartenAuf = "";

    public static void empfangen(CharakterPayload n) {
        Minecraft mc = Minecraft.getInstance();
        switch (n.art()) {
            case CharakterPayload.ALLE -> {
                CODES.clear();
                for (String teil : n.daten().split(";")) {
                    int i = teil.indexOf('=');
                    if (i > 0) {
                        try {
                            CODES.put(UUID.fromString(teil.substring(0, i)), teil.substring(i + 1));
                        } catch (IllegalArgumentException ignoriert) {
                            // kaputter Eintrag
                        }
                    }
                }
            }
            case CharakterPayload.EDITOR -> {
                editorAusstehend = true;
                editorOeffnen(true);
            }
            case CharakterPayload.WARTEN -> {
                wartenAuf = n.daten();
                if (!(mc.screen instanceof CharakterScreen) && !(mc.screen instanceof WartenScreen) && !ClientKino.aktiv()) {
                    aufschieben(mc);
                    mc.setScreen(new WartenScreen());
                }
            }
            case CharakterPayload.KINO -> {
                wartenAuf = "";
                aufschieben(mc);
                ClientKino.starten(n.daten());
            }
            default -> {
            }
        }
    }

    /** Läuft gerade die Klassenwahl, kommt sie danach wieder. */
    private static void aufschieben(Minecraft mc) {
        if (mc.screen instanceof KlassenScreen || mc.screen instanceof IntroScreen) {
            ClientKlasse.klassenwahlDanach = true;
        }
    }

    public static void editorOeffnen(boolean erstesMal) {
        Minecraft mc = Minecraft.getInstance();
        aufschieben(mc);
        mc.setScreen(new CharakterScreen(erstesMal));
    }

    /** Solange Editor, Warten oder Intro laufen, soll sich die Klassenwahl hinten anstellen. */
    public static boolean vorDemSpiel() {
        Minecraft mc = Minecraft.getInstance();
        return editorAusstehend || mc.screen instanceof CharakterScreen || mc.screen instanceof WartenScreen || ClientKino.aktiv();
    }

    /** Aus dem Editor: Aussehen übernehmen und an den Server schicken. */
    static void uebernehmen(String code, boolean erstesMal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            CODES.put(mc.player.getUUID(), code);
        }
        editorAusstehend = false;
        PacketDistributor.sendToServer(new CharakterPayload(CharakterPayload.ALLE, code));
        if (erstesMal) {
            mc.setScreen(new WartenScreen()); // kurz warten: kommt das Intro, oder warten wir auf andere?
        } else {
            weiter();
        }
    }

    /** Nach Editor/Warten/Intro: die aufgeschobene Klassenwahl, sonst zurück ins Spiel. */
    public static void weiter() {
        Minecraft mc = Minecraft.getInstance();
        if (ClientKlasse.klassenwahlDanach && !ClientKino.aktiv()) {
            ClientKlasse.klassenwahlDanach = false;
            mc.setScreen(new KlassenScreen());
        } else {
            mc.setScreen(null);
        }
    }

    public static String code(UUID u) {
        return CODES.get(u);
    }

    /** Skin-Textur zu einem Code (wird beim ersten Mal gebaut). Null bei "eigen" oder ungültig. */
    public static ResourceLocation textur(String code) {
        int[] w = CharakterTeile.lesen(code);
        if (w == null || !RenderSystem.isOnRenderThread()) {
            return null;
        }
        return TEXTUREN.computeIfAbsent(CharakterTeile.kodieren(w), c -> {
            NativeImage bild = CharakterBild.bauen(w);
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID,
                    "charakter/skin_" + c.replace('.', '_'));
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(bild));
            return rl;
        });
    }

    /** Für den Skin-Mixin und das Intro: eigener Skin eines Spielers, oder null (dann bleibt der normale). */
    public static PlayerSkin skin(UUID u, PlayerSkin original) {
        String code = CODES.get(u);
        int[] w = CharakterTeile.lesen(code);
        if (w == null) {
            return null;
        }
        ResourceLocation t = textur(code);
        if (t == null) {
            return null;
        }
        return new PlayerSkin(t, null, original.capeTexture(), original.elytraTexture(),
                w[CharakterTeile.SCHMAL] == 1 ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE, original.secure());
    }

    /** Der echte (Mojang-)Skin eines Spielers, ohne Editor. */
    public static PlayerSkin echterSkin(UUID u) {
        var c = Minecraft.getInstance().getConnection();
        var info = c == null ? null : c.getPlayerInfo(u);
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(u);
    }

    /** Skin, wie ihn alle sehen: Editor-Aussehen oder der echte Skin. */
    public static PlayerSkin sichtbarerSkin(UUID u) {
        PlayerSkin echt = echterSkin(u);
        PlayerSkin eigen = skin(u, echt);
        return eigen != null ? eigen : echt;
    }

    public static void abmelden(ClientPlayerNetworkEvent.LoggingOut e) {
        CODES.clear();
        var tm = Minecraft.getInstance().getTextureManager();
        TEXTUREN.values().forEach(tm::release);
        TEXTUREN.clear();
        editorAusstehend = false;
        wartenAuf = "";
    }
}
