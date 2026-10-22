package de.deadzonemc.client.kino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.client.charakter.ClientCharakter;
import de.deadzonemc.kino.KinoFigur;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Zeichnet die Charaktere der Mitspieler im Intro – mit ihrem Editor-Aussehen (oder echtem Skin). */
public class KinoFigurRenderer extends LivingEntityRenderer<KinoFigur, PlayerModel<KinoFigur>> {

    private final PlayerModel<KinoFigur> breit;
    private final PlayerModel<KinoFigur> schmal;

    public KinoFigurRenderer(EntityRendererProvider.Context c) {
        super(c, new KinoModell(c.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
        breit = model;
        schmal = new KinoModell(c.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    private PlayerSkin skin(KinoFigur f) {
        return ClientCharakter.sichtbarerSkin(f.besitzer);
    }

    @Override
    public void render(KinoFigur f, float gier, float teil, PoseStack ps, MultiBufferSource puffer, int licht) {
        if (f.kamera || f.besitzer == null) {
            return;
        }
        model = skin(f).model() == PlayerSkin.Model.SLIM ? schmal : breit;
        super.render(f, gier, teil, ps, puffer, licht);
    }

    @Override
    public ResourceLocation getTextureLocation(KinoFigur f) {
        return skin(f).texture();
    }

    @Override
    protected void scale(KinoFigur f, PoseStack ps, float teil) {
        ps.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected void setupRotations(KinoFigur f, PoseStack ps, float wippen, float koerperGier, float teil, float skala) {
        super.setupRotations(f, ps, wippen, koerperGier, teil, skala);
        float roll = Mth.lerp(teil, f.rollO, f.roll);
        float kipp = Mth.lerp(teil, f.kippenO, f.kippen);
        if (roll != 0 || kipp != 0) {
            ps.translate(0, 0.9, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(roll));
            ps.mulPose(Axis.XP.rotationDegrees(kipp));
            ps.translate(0, -0.9, 0);
        }
    }

    @Override
    protected boolean shouldShowName(KinoFigur f) {
        return false;
    }

    @Override
    protected int getBlockLightLevel(KinoFigur f, BlockPos pos) {
        return ClientKino.lichtBlock();
    }

    @Override
    protected int getSkyLightLevel(KinoFigur f, BlockPos pos) {
        return ClientKino.lichtHimmel();
    }
}
