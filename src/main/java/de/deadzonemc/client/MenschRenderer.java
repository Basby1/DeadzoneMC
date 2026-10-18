package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Menschen: eigenes Modell und eigene Textur je Art, aber gemeinsame Animationen (mensch.animation.json). */
public class MenschRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "animations/entity/mensch.animation.json");

    public MenschRenderer(EntityRendererProvider.Context ctx, String name, boolean leuchtet) {
        super(ctx, new DefaultedEntityGeoModel<T>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name)) {
            @Override
            public ResourceLocation getAnimationResource(T tier) {
                return ANIMATION;
            }
        });
        this.shadowRadius = 0.5F;
        if (leuchtet) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
        addRenderLayer(new GlasLayer<>(this, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID,
                "textures/entity/" + name + "_glas.png")));
    }
}
