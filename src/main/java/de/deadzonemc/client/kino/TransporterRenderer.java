package de.deadzonemc.client.kino;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.kino.Transporter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Der Transporthubschrauber aus dem Intro. */
public class TransporterRenderer extends GeoEntityRenderer<Transporter> {

    public TransporterRenderer(EntityRendererProvider.Context c) {
        super(c, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "transporter")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0;
    }

    /** GeckoLib dreht nur Lebewesen nach ihrer Blickrichtung – den Hubschrauber drehen wir selbst. */
    @Override
    public void render(Transporter t, float gier, float teil, com.mojang.blaze3d.vertex.PoseStack ps,
                       net.minecraft.client.renderer.MultiBufferSource puffer, int licht) {
        ps.pushPose();
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(teil, t.yRotO, t.getYRot())));
        super.render(t, gier, teil, ps, puffer, licht);
        ps.popPose();
    }

    @Override
    protected int getBlockLightLevel(Transporter t, BlockPos pos) {
        return ClientKino.lichtBlock();
    }

    @Override
    protected int getSkyLightLevel(Transporter t, BlockPos pos) {
        return ClientKino.lichtHimmel();
    }
}
