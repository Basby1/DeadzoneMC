package de.deadzonemc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.deadzonemc.bossmod.TrophaeenbrettBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** Zeichnet das aufgehängte Ding vor dem Trophäenbrett – Boss-Köpfe groß, alles andere wie im Rahmen. */
public class TrophaeenbrettRenderer implements BlockEntityRenderer<TrophaeenbrettBlockEntity> {

    public TrophaeenbrettRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TrophaeenbrettBlockEntity be, float teilTick, PoseStack pose, MultiBufferSource puffer, int licht, int overlay) {
        ItemStack s = be.ding();
        if (s.isEmpty()) {
            return;
        }
        Direction d = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean kopf = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().startsWith("kopf_");
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-d.toYRot()));
        // nach vorn vom Brett weg
        pose.translate(0, 0, kopf ? 0.05 : 0.38);
        float g = kopf ? 0.85F : 0.6F;
        pose.scale(g, g, g);
        Minecraft.getInstance().getItemRenderer().renderStatic(s, ItemDisplayContext.FIXED, licht, overlay, pose, puffer,
                be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }
}
