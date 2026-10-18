package de.deadzonemc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.deadzonemc.monster.Mimik;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Mimik: getarnt als Mitspieler (Spielermodell mit dessen Haut und Namensschild), enthüllt als GeckoLib-Monster.
 */
public class MimikRenderer extends HumanoidMobRenderer<Mimik, PlayerModel<Mimik>> {

    private final MonsterRenderer<Mimik> monster;

    public MimikRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.monster = new MonsterRenderer<>(ctx, "mimik", false, false, 0.5F);
    }

    @Override
    public void render(Mimik m, float gier, float teil, PoseStack pose, MultiBufferSource puffer, int licht) {
        if (m.enthuellt()) {
            monster.render(m, gier, teil, pose, puffer, licht);
            return;
        }
        super.render(m, gier, teil, pose, puffer, licht);
    }

    @Override
    public ResourceLocation getTextureLocation(Mimik m) {
        var verbindung = Minecraft.getInstance().getConnection();
        if (m.vorbild().isPresent() && verbindung != null) {
            PlayerInfo info = verbindung.getPlayerInfo(m.vorbild().get());
            if (info != null) {
                return info.getSkin().texture();
            }
            return DefaultPlayerSkin.get(m.vorbild().get()).texture();
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
