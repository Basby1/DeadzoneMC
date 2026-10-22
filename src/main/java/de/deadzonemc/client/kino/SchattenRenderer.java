package de.deadzonemc.client.kino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.kino.UrsprungSchatten;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Der Ursprung als Schatten: sechsmal so groß, fast schwarz – und wenn er das Auge öffnet, glüht es violett. */
public class SchattenRenderer extends GeoEntityRenderer<UrsprungSchatten> {

    private static final ResourceLocation AUGE = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID,
            "textures/entity/der_ursprung_schatten_glowmask.png");

    private static final class Modell extends GeoModel<UrsprungSchatten> {
        @Override
        public ResourceLocation getModelResource(UrsprungSchatten s) {
            return ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "geo/entity/der_ursprung.geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(UrsprungSchatten s) {
            return ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "textures/entity/der_ursprung_schatten.png");
        }

        @Override
        public ResourceLocation getAnimationResource(UrsprungSchatten s) {
            return ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "animations/entity/der_ursprung.animation.json");
        }
    }

    public SchattenRenderer(EntityRendererProvider.Context c) {
        super(c, new Modell());
        withScale(6.0F);
        shadowRadius = 0;
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, UrsprungSchatten s, BakedGeoModel modell, RenderType typ, MultiBufferSource puffer,
                               VertexConsumer vc, float teil, int licht, int overlay) {
                if (!s.augeOffen) {
                    return;
                }
                RenderType auge = RenderType.eyes(AUGE);
                getRenderer().reRender(modell, pose, puffer, s, auge, puffer.getBuffer(auge), teil, 0xF000F0, overlay, 0xFFFFFFFF);
            }
        });
    }

    @Override
    public void render(UrsprungSchatten s, float gier, float teil, PoseStack ps, MultiBufferSource puffer, int licht) {
        if (s.sichtbar > 0.01F) {
            ps.pushPose();
            ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(teil, s.yRotO, s.getYRot())));
            super.render(s, gier, teil, ps, puffer, licht);
            ps.popPose();
        }
    }

    @Override
    protected int getBlockLightLevel(UrsprungSchatten s, BlockPos pos) {
        return 6;
    }

    @Override
    protected int getSkyLightLevel(UrsprungSchatten s, BlockPos pos) {
        return 6;
    }
}
