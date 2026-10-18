package de.deadzonemc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Zweiter Zeichendurchgang für halbdurchsichtige Teile (Glashelm, Auren, Schutzschilde). Der Generator legt diese
 * Pixel in {@code <name>_glas.png} ab; das Hauptmodell wird ausgeschnitten gezeichnet, danach kommt diese Textur
 * durchscheinend darüber – so verdecken Schilde und Auren nicht mehr den Körper darunter.
 */
public class GlasLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {

    private final ResourceLocation textur;
    private Boolean vorhanden;

    public GlasLayer(GeoRenderer<T> renderer, ResourceLocation textur) {
        super(renderer);
        this.textur = textur;
    }

    @Override
    public void render(PoseStack pose, T tier, BakedGeoModel modell, RenderType typ, MultiBufferSource puffer,
                       VertexConsumer vc, float teilTick, int licht, int overlay) {
        if (vorhanden == null) {
            vorhanden = Minecraft.getInstance().getResourceManager().getResource(textur).isPresent();
        }
        if (!vorhanden) {
            return;
        }
        RenderType glas = RenderType.entityTranslucent(textur);
        getRenderer().reRender(modell, pose, puffer, tier, glas, puffer.getBuffer(glas), teilTick, licht, overlay, 0xFFFFFFFF);
    }
}
