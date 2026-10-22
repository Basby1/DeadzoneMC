package de.deadzonemc.client.charakter;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.deadzonemc.client.charakter.CharakterTeile.*;

/**
 * Der Charakter-Editor: links die Figur (dreht sich, mit der Maus ziehen), rechts drei Reiter mit allen Teilen.
 * Beim ersten Start kommt er vor dem Intro und der Klassenwahl.
 */
public class CharakterScreen extends Screen {

    private static final ResourceLocation VORSCHAU = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "charakter/vorschau");
    private static final int ROT = 0xFFB02818;
    private static final int HELL = 0xFFE8DCB8;
    private static final int GRAU = 0xFF9A958A;

    private record Ziel(int x, int y, int b, int h, Runnable aktion) {
        boolean trifft(double mx, double my) {
            return mx >= x && mx < x + b && my >= y && my < y + h;
        }
    }

    private final boolean erstesMal;
    private final List<Ziel> ziele = new ArrayList<>();
    private int[] wahl;
    private boolean eigen;
    private int reiter;
    private float drehung = 200;
    private boolean ziehen;
    private int zeit;
    private DynamicTexture textur;
    private PlayerModel<AbstractClientPlayer> breit;
    private PlayerModel<AbstractClientPlayer> schmal;

    public CharakterScreen(boolean erstesMal) {
        super(Component.literal("Charakter"));
        this.erstesMal = erstesMal;
        Minecraft mc = Minecraft.getInstance();
        String code = mc.player == null ? null : ClientCharakter.code(mc.player.getUUID());
        int[] w = CharakterTeile.lesen(code);
        this.eigen = "eigen".equals(code);
        this.wahl = w != null ? w : CharakterTeile.zufall(RandomSource.create());
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (breit == null) {
            breit = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            schmal = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        if (textur == null) {
            textur = new DynamicTexture(64, 64, true);
            mc.getTextureManager().register(VORSCHAU, textur);
            neuBauen();
        }
    }

    private void neuBauen() {
        if (textur != null && textur.getPixels() != null) {
            CharakterBild.bauenIn(wahl, textur.getPixels());
            textur.upload();
        }
    }

    private void aendern(int stelle, int schritt) {
        wahl[stelle] = Math.floorMod(wahl[stelle] + schritt, CharakterTeile.anzahl(stelle));
        eigen = false;
        neuBauen();
    }

    private void setzen(int stelle, int wert) {
        wahl[stelle] = wert;
        eigen = false;
        neuBauen();
    }

    @Override
    public void tick() {
        zeit++;
        if (!ziehen) {
            drehung += 0.6F;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float d) {
        g.fillGradient(0, 0, width, height, 0xE80C0E10, 0xF4050607);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float d) {
        renderBackground(g, mx, my, d);
        ziele.clear();
        int gesamt = Math.min(width - 20, 400);
        int links = (width - gesamt) / 2;
        int vorschauB = Math.min(150, gesamt * 36 / 100);
        int sx = links + vorschauB + 18;
        int sb = gesamt - vorschauB - 18;
        boolean eng = height < 280;

        // Titel
        g.pose().pushPose();
        g.pose().translate(width / 2F, 10, 0);
        g.pose().scale(2, 2, 1);
        g.drawCenteredString(font, Component.literal("WER BIST DU?"), 0, 0, ROT);
        g.pose().popPose();
        g.drawCenteredString(font, Component.literal(erstesMal
                ? "Bevor es losgeht: Wie siehst du aus? Die anderen sehen dich genau so."
                : "Dein Aussehen – die anderen sehen dich genau so."), width / 2, 30, GRAU);

        // Vorschau
        int top = 46;
        int unten = height - 34;
        g.fill(links, top, links + vorschauB, unten, 0x40000000);
        g.renderOutline(links, top, vorschauB, unten - top, 0x30FFFFFF);
        figur(g, links + vorschauB / 2, top + (unten - top) / 2, (unten - top) * 0.38F, d);
        ziele.add(new Ziel(links, top, vorschauB, unten - top, () -> ziehen = true));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            g.drawCenteredString(font, mc.player.getName(), links + vorschauB / 2, unten - 12, HELL);
        }

        // Reiter
        String[] reiterN = {"KOPF", "KLEIDUNG", "AUSRÜSTUNG"};
        int rb = sb / 3;
        for (int i = 0; i < 3; i++) {
            int x = sx + i * rb;
            boolean aktiv = i == reiter;
            boolean ueber = mx >= x && mx < x + rb - 2 && my >= top && my < top + 14;
            g.fill(x, top, x + rb - 2, top + 14, aktiv ? 0xC0401410 : ueber ? 0x60402420 : 0x40202020);
            g.drawCenteredString(font, reiterN[i], x + rb / 2 - 1, top + 3, aktiv ? HELL : GRAU);
            int r = i;
            ziele.add(new Ziel(x, top, rb - 2, 14, () -> reiter = r));
        }

        int y = top + 20;
        int zeile = eng ? 24 : 27;
        switch (reiter) {
            case 0 -> {
                y = farben(g, mx, my, sx, y, "Hautton", HAUT, HAUTFARBEN, zeile);
                y = farben(g, mx, my, sx, y, "Augen", AUGEN, AUGENFARBEN, zeile);
                y = auswahl(g, mx, my, sx, y, sb, "Gesicht", GESICHT, GESICHTER_N, zeile);
                y = auswahl(g, mx, my, sx, y, sb, "Frisur", HAARE, HAARE_N, zeile);
                y = farben(g, mx, my, sx, y, "Haarfarbe", HAARFARBE, HAARFARBEN, zeile);
                auswahl(g, mx, my, sx, y, sb, "Bart", BART, BAERTE_N, zeile);
            }
            case 1 -> {
                y = auswahl(g, mx, my, sx, y, sb, "Oberteil", OBERTEIL, OBERTEILE_N, 15);
                y = farben(g, mx, my, sx, y, "", OBERFARBE, STOFFFARBEN, zeile);
                y = auswahl(g, mx, my, sx, y, sb, "Hose", HOSE, HOSEN_N, 15);
                y = farben(g, mx, my, sx, y, "", HOSENFARBE, STOFFFARBEN, zeile);
                y = auswahl(g, mx, my, sx, y, sb, "Schuhe", SCHUHE, SCHUHE_N, 15);
                farben(g, mx, my, sx, y, "", SCHUHFARBE, STOFFFARBEN, zeile);
            }
            default -> {
                y = auswahl(g, mx, my, sx, y, sb, "Kopf", KOPF, KOEPFE_N, 15);
                y = farben(g, mx, my, sx, y, "", KOPFFARBE, STOFFFARBEN, zeile);
                y = auswahl(g, mx, my, sx, y, sb, "Am Körper", EXTRA, EXTRAS_N, 15);
                y = farben(g, mx, my, sx, y, "", EXTRAFARBE, STOFFFARBEN, zeile);
                auswahl(g, mx, my, sx, y, sb, "Arme", SCHMAL, ARME_N, zeile);
            }
        }

        // Knöpfe unten
        int kb = (sb - 8) / 3;
        int ky = height - 26;
        knopf(g, mx, my, sx, ky, kb, "Zufall", () -> {
            wahl = CharakterTeile.zufall(RandomSource.create());
            eigen = false;
            neuBauen();
        });
        knopf(g, mx, my, sx + kb + 4, ky, kb, eigen ? "§aEigener Skin ✔" : "Eigener Skin", () -> eigen = !eigen);
        knopf(g, mx, my, sx + 2 * (kb + 4), ky, kb, "§lFertig", () ->
                ClientCharakter.uebernehmen(eigen ? "eigen" : CharakterTeile.kodieren(wahl), erstesMal));
        if (eigen) {
            g.drawCenteredString(font, Component.literal("Du spielst mit deinem eigenen Minecraft-Skin."), sx + sb / 2, ky - 12, 0xFF80C080);
        }
        super.render(g, mx, my, d);
    }

    private void knopf(GuiGraphics g, int mx, int my, int x, int y, int b, String text, Runnable r) {
        boolean ueber = mx >= x && mx < x + b && my >= y && my < y + 18;
        g.fill(x, y, x + b, y + 18, ueber ? 0xD0581C14 : 0xA0301010);
        g.renderOutline(x, y, b, 18, ueber ? 0xFFB02818 : 0x80602018);
        g.drawCenteredString(font, Component.literal(text), x + b / 2, y + 5, HELL);
        ziele.add(new Ziel(x, y, b, 18, r));
    }

    /** Zeile mit ◀ Name ▶. */
    private int auswahl(GuiGraphics g, int mx, int my, int x, int y, int b, String titel, int stelle, String[] namen, int hoehe) {
        g.drawString(font, titel, x, y, GRAU);
        int sy = y + 10;
        int pfeil = 14;
        pfeilKnopf(g, mx, my, x, sy, pfeil, "◀", () -> aendern(stelle, -1));
        pfeilKnopf(g, mx, my, x + b - pfeil, sy, pfeil, "▶", () -> aendern(stelle, 1));
        g.fill(x + pfeil + 2, sy, x + b - pfeil - 2, sy + 12, 0x50000000);
        String name = namen[wahl[stelle]];
        g.drawCenteredString(font, name, x + b / 2, sy + 2, eigen ? GRAU : HELL);
        g.drawString(font, (wahl[stelle] + 1) + "/" + namen.length, x + b - pfeil - 26, sy + 2, 0xFF5A564E, false);
        return y + hoehe;
    }

    private void pfeilKnopf(GuiGraphics g, int mx, int my, int x, int y, int b, String s, Runnable r) {
        boolean ueber = mx >= x && mx < x + b && my >= y && my < y + 12;
        g.fill(x, y, x + b, y + 12, ueber ? 0xD0581C14 : 0x90301010);
        g.drawCenteredString(font, s, x + b / 2, y + 2, HELL);
        ziele.add(new Ziel(x, y, b, 12, r));
    }

    /** Zeile mit Farbfeldern. */
    private int farben(GuiGraphics g, int mx, int my, int x, int y, String titel, int stelle, int[] farben, int hoehe) {
        if (!titel.isEmpty()) {
            g.drawString(font, titel, x, y, GRAU);
        }
        int sy = y + 10;
        for (int i = 0; i < farben.length; i++) {
            int fx = x + i * 13;
            boolean gewaehlt = wahl[stelle] == i;
            boolean ueber = mx >= fx && mx < fx + 11 && my >= sy && my < sy + 11;
            g.fill(fx - 1, sy - 1, fx + 12, sy + 12, gewaehlt ? 0xFFE8DCB8 : ueber ? 0xFF8A8478 : 0xFF202020);
            g.fill(fx, sy, fx + 11, sy + 11, 0xFF000000 | farben[i]);
            int wert = i;
            ziele.add(new Ziel(fx, sy, 11, 11, () -> setzen(stelle, wert)));
        }
        return y + hoehe;
    }

    private void figur(GuiGraphics g, int cx, int cy, float groesse, float d) {
        Minecraft mc = Minecraft.getInstance();
        boolean duenn = wahl[SCHMAL] == 1;
        ResourceLocation tex = VORSCHAU;
        if (eigen && mc.player != null) {
            UUID u = mc.player.getUUID();
            PlayerSkin s = ClientCharakter.echterSkin(u);
            tex = s.texture();
            duenn = s.model() == PlayerSkin.Model.SLIM;
        }
        PlayerModel<AbstractClientPlayer> m = duenn ? schmal : breit;
        float t = zeit + d;
        m.head.resetPose();
        m.body.resetPose();
        m.rightArm.resetPose();
        m.leftArm.resetPose();
        m.rightLeg.resetPose();
        m.leftLeg.resetPose();
        m.head.yRot = Mth.sin(t * 0.035F) * 0.35F;
        m.head.xRot = Mth.sin(t * 0.05F) * 0.06F;
        m.rightArm.zRot = 0.07F + Mth.sin(t * 0.067F) * 0.04F;
        m.leftArm.zRot = -0.07F - Mth.sin(t * 0.067F) * 0.04F;
        m.rightArm.xRot = Mth.sin(t * 0.05F) * 0.04F;
        m.leftArm.xRot = -Mth.sin(t * 0.05F) * 0.04F;
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy - groesse * 0.5F, 150);
        ps.scale(groesse, groesse, -groesse);
        ps.mulPose(Axis.XP.rotationDegrees(-6));
        ps.mulPose(Axis.YP.rotationDegrees(drehung));
        Lighting.setupForEntityInInventory();
        var puffer = g.bufferSource();
        m.renderToBuffer(ps, puffer.getBuffer(RenderType.entityTranslucent(tex)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        g.flush();
        Lighting.setupFor3DItems();
        ps.popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int knopf) {
        for (Ziel z : List.copyOf(ziele)) {
            if (z.trifft(mx, my)) {
                if (z.aktion() != null) {
                    z.aktion().run();
                }
                if (!(Minecraft.getInstance().screen == this)) {
                    return true;
                }
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F, 0.5F));
                return true;
            }
        }
        return super.mouseClicked(mx, my, knopf);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int knopf, double dx, double dy) {
        if (ziehen) {
            drehung += (float) dx * 2.0F;
            return true;
        }
        return super.mouseDragged(mx, my, knopf, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int knopf) {
        ziehen = false;
        return super.mouseReleased(mx, my, knopf);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !erstesMal;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        Minecraft.getInstance().getTextureManager().release(VORSCHAU);
        textur = null;
    }
}
