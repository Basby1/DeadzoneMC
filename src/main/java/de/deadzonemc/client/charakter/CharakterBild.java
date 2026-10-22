package de.deadzonemc.client.charakter;

import com.mojang.blaze3d.platform.NativeImage;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.deadzonemc.client.charakter.CharakterTeile.*;

/** Setzt aus den Ebenen (assets/deadzonemc/textures/charakter/…) einen 64x64-Skin zusammen – wie werkzeuge/charakter.py. */
public final class CharakterBild {

    private CharakterBild() {
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** Geladene Ebenen (null = gibt es nicht). */
    private static final Map<String, Optional<NativeImage>> EBENEN = new HashMap<>();

    private static NativeImage ebene(String pfad) {
        return EBENEN.computeIfAbsent(pfad, p -> {
            var rl = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "textures/charakter/" + p + ".png");
            var res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) {
                return Optional.empty();
            }
            try (InputStream in = res.get().open()) {
                return Optional.of(NativeImage.read(in));
            } catch (Exception e) {
                LOG.warn("Charakter-Ebene {} nicht lesbar", rl, e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    public static NativeImage bauen(int[] w) {
        NativeImage aus = new NativeImage(64, 64, true);
        bauenIn(w, aus);
        return aus;
    }

    public static void bauenIn(int[] w, NativeImage aus) {
        aus.fillRect(0, 0, 64, 64, 0);
        boolean schmal = w[SCHMAL] == 1;
        int haar = HAARFARBEN[w[HAARFARBE]];
        legen(aus, "haut/haut", schmal, HAUTFARBEN[w[HAUT]]);
        legen(aus, "gesicht/" + GESICHTER[w[GESICHT]], schmal, haar);
        legen(aus, "augen/augen", schmal, AUGENFARBEN[w[AUGEN]]);
        legen(aus, "bart/" + BAERTE[w[BART]], schmal, haar);
        legen(aus, "hose/" + HOSEN[w[HOSE]], schmal, STOFFFARBEN[w[HOSENFARBE]]);
        legen(aus, "schuhe/" + SCHUHE_[w[SCHUHE]], schmal, STOFFFARBEN[w[SCHUHFARBE]]);
        legen(aus, "oberteil/" + OBERTEILE[w[OBERTEIL]], schmal, STOFFFARBEN[w[OBERFARBE]]);
        legen(aus, "haare/" + HAARE_[w[HAARE]], schmal, haar);
        legen(aus, "extra/" + EXTRAS[w[EXTRA]], schmal, STOFFFARBEN[w[EXTRAFARBE]]);
        legen(aus, "kopf/" + KOEPFE[w[KOPF]], schmal, STOFFFARBEN[w[KOPFFARBE]]);
    }

    private static void legen(NativeImage aus, String name, boolean schmal, int farbe) {
        String s = schmal ? "_s" : "";
        NativeImage f = ebene(name + s);
        if (f != null) {
            mischen(aus, f, farbe);
        }
        NativeImage d = ebene(name + s + "_d");
        if (d != null) {
            mischen(aus, d, -1);
        }
    }

    /** Legt {@code oben} über {@code aus}; bei farbe >= 0 wird der Grauwert mit der Farbe multipliziert. */
    private static void mischen(NativeImage aus, NativeImage oben, int farbe) {
        int fr = (farbe >> 16) & 0xFF, fg = (farbe >> 8) & 0xFF, fb = farbe & 0xFF;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int o = oben.getPixelRGBA(x, y); // ABGR
                int oa = (o >>> 24) & 0xFF;
                if (oa == 0) {
                    continue;
                }
                int or = o & 0xFF, og = (o >> 8) & 0xFF, ob = (o >> 16) & 0xFF;
                if (farbe >= 0) {
                    or = fr * or / 255;
                    og = fg * og / 255;
                    ob = fb * ob / 255;
                }
                int u = aus.getPixelRGBA(x, y);
                int ua = (u >>> 24) & 0xFF;
                int ur = u & 0xFF, ug = (u >> 8) & 0xFF, ub = (u >> 16) & 0xFF;
                int na = oa + ua * (255 - oa) / 255;
                if (na == 0) {
                    continue;
                }
                int nr = (or * oa + ur * ua * (255 - oa) / 255) / na;
                int ng = (og * oa + ug * ua * (255 - oa) / 255) / na;
                int nb = (ob * oa + ub * ua * (255 - oa) / 255) / na;
                aus.setPixelRGBA(x, y, (na << 24) | (nb << 16) | (ng << 8) | nr);
            }
        }
    }
}
