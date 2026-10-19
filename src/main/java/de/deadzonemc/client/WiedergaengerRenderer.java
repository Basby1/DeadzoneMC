package de.deadzonemc.client;

import de.deadzonemc.monster.Wiedergaenger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Der Wiedergänger: Spielermodell mit der Haut des Verstorbenen, Arme ausgestreckt wie ein Zombie, mit Rüstung. */
public class WiedergaengerRenderer extends HumanoidMobRenderer<Wiedergaenger, PlayerModel<Wiedergaenger>> {

    public WiedergaengerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false) {
            @Override
            public void setupAnim(Wiedergaenger e, float gang, float tempo, float alter, float kopfY, float kopfX) {
                super.setupAnim(e, gang, tempo, alter, kopfY, kopfX);
                float wanken = (float) Math.sin(alter * 0.07F) * 0.08F;
                rightArm.xRot = -1.45F + wanken;
                leftArm.xRot = -1.35F - wanken;
                rightArm.zRot = -0.1F;
                leftArm.zRot = 0.1F;
                head.zRot = 0.18F;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
                hat.copyFrom(head);
            }
        }, 0.5F);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(Wiedergaenger w) {
        var verbindung = Minecraft.getInstance().getConnection();
        if (w.vorbild().isPresent()) {
            if (verbindung != null) {
                PlayerInfo info = verbindung.getPlayerInfo(w.vorbild().get());
                if (info != null) {
                    return info.getSkin().texture();
                }
            }
            return DefaultPlayerSkin.get(w.vorbild().get()).texture();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
