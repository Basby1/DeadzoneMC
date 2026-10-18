package de.deadzonemc.client;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Zeichnet ein Deadzone-Monster mit GeckoLib: Modell, Textur, Animationen und leuchtende Stellen. */
public class MonsterRenderer<T extends DeadzoneMonster> extends GeoEntityRenderer<T> {

    private final boolean durchscheinend;

    public MonsterRenderer(EntityRendererProvider.Context ctx, String name, boolean leuchtet, boolean durchscheinend,
                           float schatten) {
        super(ctx, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name)));
        this.durchscheinend = durchscheinend;
        this.shadowRadius = schatten;
        if (leuchtet) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
        if (!durchscheinend) {
            addRenderLayer(new GlasLayer<>(this, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID,
                    "textures/entity/" + name + "_glas.png")));
        }
    }

    @Override
    public RenderType getRenderType(T tier, ResourceLocation textur, MultiBufferSource puffer, float teilTick) {
        return durchscheinend ? RenderType.entityTranslucent(textur) : super.getRenderType(tier, textur, puffer, teilTick);
    }
}
