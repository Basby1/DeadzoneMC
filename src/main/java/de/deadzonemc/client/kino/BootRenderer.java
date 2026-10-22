package de.deadzonemc.client.kino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.kino.KinoBoot;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Das Patrouillenboot aus dem Boot-Intro – gedreht, geneigt und gerollt vom Drehbuch. */
public class BootRenderer extends GeoEntityRenderer<KinoBoot> {

    public BootRenderer(EntityRendererProvider.Context c) {
        super(c, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kino_boot")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0;
    }

    @Override
    public void render(KinoBoot b, float gier, float teil, PoseStack ps, MultiBufferSource puffer, int licht) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(teil, b.yRotO, b.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(teil, b.nickO, b.nick)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(teil, b.rollO, b.roll)));
        super.render(b, gier, teil, ps, puffer, licht);
        ps.popPose();
    }

    @Override
    protected int getBlockLightLevel(KinoBoot b, BlockPos pos) {
        return ClientKinoBoot.lichtBlock();
    }

    @Override
    protected int getSkyLightLevel(KinoBoot b, BlockPos pos) {
        return ClientKinoBoot.lichtHimmel();
    }
}
