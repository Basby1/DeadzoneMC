package de.deadzonemc.client.kino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.kino.KinoRuinen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Die zerstörte Stadt am Ufer: im Viertelmaßstab gebaut, hier vierfach vergrößert. */
public class RuinenRenderer extends GeoEntityRenderer<KinoRuinen> {

    public RuinenRenderer(EntityRendererProvider.Context c) {
        super(c, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kino_ruinen")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        withScale(4.0F);
        shadowRadius = 0;
    }

    @Override
    public void render(KinoRuinen r, float gier, float teil, PoseStack ps, MultiBufferSource puffer, int licht) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-r.getYRot()));
        super.render(r, gier, teil, ps, puffer, licht);
        ps.popPose();
    }

    @Override
    protected int getBlockLightLevel(KinoRuinen r, BlockPos pos) {
        return ClientKinoBoot.lichtBlock() > 10 ? 15 : 5;
    }

    @Override
    protected int getSkyLightLevel(KinoRuinen r, BlockPos pos) {
        return ClientKinoBoot.lichtHimmel() > 10 ? 15 : 4;
    }
}
