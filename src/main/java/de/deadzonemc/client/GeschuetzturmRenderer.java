package de.deadzonemc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.falle.Fallen;
import de.deadzonemc.falle.GeschuetzturmBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Dreht den Kopf des Geschützturms zum Ziel (das Kopfmodell ist ein eigenes Item-Modell). */
public class GeschuetzturmRenderer implements BlockEntityRenderer<GeschuetzturmBlockEntity> {

    private ItemStack kopf;

    public GeschuetzturmRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(GeschuetzturmBlockEntity t, float delta, PoseStack pose, MultiBufferSource puffer, int licht, int overlay) {
        if (kopf == null) {
            kopf = new ItemStack(Fallen.GESCHUETZTURM_KOPF.get());
        }
        pose.pushPose();
        pose.translate(0.5, 13.0 / 16.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(delta, t.drehungAlt, t.drehung)));
        if (t.feuer > 0) {
            pose.translate(0, 0, 0.04); // Rückstoß
        }
        Minecraft.getInstance().getItemRenderer().renderStatic(kopf, ItemDisplayContext.FIXED, licht, overlay, pose, puffer,
                t.getLevel(), 0);
        pose.popPose();
    }
}
